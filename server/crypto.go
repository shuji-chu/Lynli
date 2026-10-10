package main

import (
"crypto/cipher"
"crypto/rand"
"crypto/sha256"
"encoding/binary"
"io"

"golang.org/x/crypto/chacha20poly1305"
"golang.org/x/crypto/hkdf"
)

func newAEAD(password string, salt []byte) (cipher.AEAD, error) {
r := hkdf.New(sha256.New, []byte(password), salt, []byte("Lynli v0.2"))
key := make([]byte, 32)
if _, err := io.ReadFull(r, key); err != nil {
return nil, err
}
return chacha20poly1305.New(key)
}

func writeFrame(w io.Writer, aead cipher.AEAD, plaintext []byte) error {
nonce := make([]byte, aead.NonceSize())
if _, err := rand.Read(nonce); err != nil {
return err
}
ciphertext := aead.Seal(nil, nonce, plaintext, nil)
total := len(nonce) + len(ciphertext)
buf := make([]byte, 2+total)
binary.BigEndian.PutUint16(buf[:2], uint16(total))
copy(buf[2:], nonce)
copy(buf[2+len(nonce):], ciphertext)
_, err := w.Write(buf)
return err
}

func readFrame(r io.Reader, aead cipher.AEAD) ([]byte, error) {
lenBuf := make([]byte, 2)
if _, err := io.ReadFull(r, lenBuf); err != nil {
return nil, err
}
total := int(binary.BigEndian.Uint16(lenBuf))
body := make([]byte, total)
if _, err := io.ReadFull(r, body); err != nil {
return nil, err
}
ns := aead.NonceSize()
if total < ns {
return nil, io.ErrUnexpectedEOF
}
return aead.Open(nil, body[:ns], body[ns:], nil)
}

func relayEncrypt(dst, src io.ReadWriteCloser, aead cipher.AEAD) {
buf := make([]byte, 16*1024)
for {
n, err := src.Read(buf)
if err != nil {
return
}
if err := writeFrame(dst, aead, buf[:n]); err != nil {
return
}
}
}

func relayDecrypt(dst io.Writer, src io.ReadWriteCloser, aead cipher.AEAD) {
for {
plain, err := readFrame(src, aead)
if err != nil {
return
}
if _, err := dst.Write(plain); err != nil {
return
}
}
}
