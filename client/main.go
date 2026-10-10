package main

import (
"encoding/binary"
"fmt"
"io"
"net"
"os"

utls "github.com/refraction-networking/utls"
)

const (
Version = 0x01
AuthStr = "ys2026pass"
)

func main() {
if len(os.Args) < 4 {
fmt.Println("用法: go run . <服务器> <域名> <端口>")
return
}
server := os.Args[1]
host := os.Args[2]
port := os.Args[3]

rawConn, err := net.Dial("tcp", server)
if err != nil {
panic(err)
}

cfg := &utls.Config{
ServerName:         "amd.com",
InsecureSkipVerify: true,
}
conn := utls.UClient(rawConn, cfg, utls.HelloChrome_Auto)
if err := conn.Handshake(); err != nil {
panic(err)
}
defer conn.Close()

salt := make([]byte, 32)
if _, err := io.ReadFull(conn, salt); err != nil {
panic(err)
}

aead, err := newAEAD(AuthStr, salt)
if err != nil {
panic(err)
}

hs := []byte("LYNL")
hs = append(hs, Version)
p := 0
fmt.Sscanf(port, "%d", &p)
portBuf := make([]byte, 2)
binary.BigEndian.PutUint16(portBuf, uint16(p))
hs = append(hs, portBuf...)
hs = append(hs, 2)
hs = append(hs, byte(len(host)))
hs = append(hs, []byte(host)...)

if err := writeFrame(conn, aead, hs); err != nil {
panic(err)
}

resp, err := readFrame(conn, aead)
if err != nil {
panic(err)
}
if len(resp) < 2 || resp[1] != 0 {
fmt.Println("服务端拒绝")
return
}
fmt.Println("已连接，开始发 HTTP 请求")

req := fmt.Sprintf("GET / HTTP/1.0\r\nHost: %s\r\n\r\n", host)
if err := writeFrame(conn, aead, []byte(req)); err != nil {
panic(err)
}

for {
plain, err := readFrame(conn, aead)
if err != nil {
break
}
os.Stdout.Write(plain)
}
}
