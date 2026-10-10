package main

import (
"encoding/binary"
"fmt"
"io"
"net"
)

const (
Magic   = "LYNL"
Version = 0x01
AuthStr = "ys2026pass"
)

func main() {
ln, err := net.Listen("tcp", ":9000")
if err != nil {
panic(err)
}
fmt.Println("Lynli 服务端正在监听 :9000")

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
fmt.Println("有客户端连进来了:", conn.RemoteAddr())

// 1. 读 Magic
magic := make([]byte, 4)
if _, err := io.ReadFull(conn, magic); err != nil {
return
}
if string(magic) != Magic {
fmt.Println("Magic 不对")
return
}

// 2. 读 Version
ver := make([]byte, 1)
io.ReadFull(conn, ver)

// 3. 读 Auth
authLenBuf := make([]byte, 1)
io.ReadFull(conn, authLenBuf)
auth := make([]byte, int(authLenBuf[0]))
io.ReadFull(conn, auth)
if string(auth) != AuthStr {
fmt.Println("密码错误")
return
}

// 4. 读目标端口 (2字节)
portBuf := make([]byte, 2)
io.ReadFull(conn, portBuf)
port := binary.BigEndian.Uint16(portBuf)

// 5. 读地址类型 (1字节)
atyp := make([]byte, 1)
io.ReadFull(conn, atyp)

// 6. 读目标地址
var host string
switch atyp[0] {
case 1: // IPv4
ip := make([]byte, 4)
io.ReadFull(conn, ip)
host = net.IP(ip).String()
case 2: // 域名
lenBuf := make([]byte, 1)
io.ReadFull(conn, lenBuf)
domain := make([]byte, int(lenBuf[0]))
io.ReadFull(conn, domain)
host = string(domain)
default:
fmt.Println("不支持的地址类型")
return
}

target := fmt.Sprintf("%s:%d", host, port)
fmt.Println("准备连接目标:", target)

// 7. 连目标
remote, err := net.Dial("tcp", target)
if err != nil {
fmt.Println("连接目标失败:", err)
conn.Write([]byte{Version, 1}) // 状态 1 = 失败
return
}
defer remote.Close()

// 8. 回复成功
conn.Write([]byte{Version, 0}) // 状态 0 = 成功
fmt.Println("转发开始！")

// 9. 双向转发
go io.Copy(remote, conn)
io.Copy(conn, remote)
}
