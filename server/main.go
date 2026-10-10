package main

import (
"crypto/rand"
"crypto/tls"
"encoding/binary"
"fmt"
"net"
)

const (
Version = 0x01
AuthStr = "ys2026pass"
)

func main() {
cert, err := tls.LoadX509KeyPair("certs/server.crt", "certs/server.key")
if err != nil {
panic(err)
}
cfg := &tls.Config{
Certificates: []tls.Certificate{cert},
MinVersion:   tls.VersionTLS13,
}
ln, err := tls.Listen("tcp", ":9000", cfg)
if err != nil {
panic(err)
}
fmt.Println("Lynli v0.3 服务端监听 :9000 (TLS)")

for {
conn, err := ln.Accept()
if err != nil {
continue
}
go handle(conn)
}
}

func handle(conn net.Conn) {
defer conn.Close()
fmt.Println("客户端:", conn.RemoteAddr())

salt := make([]byte, 32)
rand.Read(salt)
if _, err := conn.Write(salt); err != nil {
return
}

aead, err := newAEAD(AuthStr, salt)
if err != nil {
fmt.Println("派生密钥失败:", err)
return
}

hs, err := readFrame(conn, aead)
if err != nil {
fmt.Println("读握手失败:", err)
return
}
if len(hs) < 8 || string(hs[:4]) != "LYNL" {
fmt.Println("Magic 不对")
return
}
if hs[4] != Version {
fmt.Println("版本不对")
return
}
port := binary.BigEndian.Uint16(hs[5:7])
atyp := hs[7]
addrBytes := hs[8:]

var host string
switch atyp {
case 1:
if len(addrBytes) < 4 {
return
}
host = net.IP(addrBytes[:4]).String()
case 2:
if len(addrBytes) < 1 {
return
}
l := int(addrBytes[0])
if len(addrBytes) < 1+l {
return
}
host = string(addrBytes[1 : 1+l])
default:
fmt.Println("不支持地址类型")
return
}

target := fmt.Sprintf("%s:%d", host, port)
fmt.Println("目标:", target)

remote, err := net.Dial("tcp", target)
if err != nil {
fmt.Println("连接失败:", err)
writeFrame(conn, aead, []byte{Version, 1})
return
}
defer remote.Close()

if err := writeFrame(conn, aead, []byte{Version, 0}); err != nil {
return
}
fmt.Println("转发开始")

go relayEncrypt(conn, remote, aead)
relayDecrypt(remote, conn, aead)
}
