package main

import (
"encoding/binary"
"fmt"
"io"
"net"
"os"
)

const (
Magic   = "LYNL"
Version = 0x01
AuthStr = "ys2026pass"
)

func main() {
if len(os.Args) < 4 {
fmt.Println("用法: go run client/main.go <服务器> <目标域名> <目标端口>")
return
}
server := os.Args[1]
host := os.Args[2]
port := os.Args[3]

conn, err := net.Dial("tcp", server)
if err != nil {
panic(err)
}
defer conn.Close()

// 1. 发握手
conn.Write([]byte(Magic))
conn.Write([]byte{Version})
conn.Write([]byte{byte(len(AuthStr))})
conn.Write([]byte(AuthStr))

// 2. 发目标端口
p := 0
fmt.Sscanf(port, "%d", &p)
portBuf := make([]byte, 2)
binary.BigEndian.PutUint16(portBuf, uint16(p))
conn.Write(portBuf)

// 3. 发地址类型和地址（这里假设是域名）
conn.Write([]byte{2})
conn.Write([]byte{byte(len(host))})
conn.Write([]byte(host))

// 4. 读服务端响应
resp := make([]byte, 2)
io.ReadFull(conn, resp)
if resp[1] != 0 {
fmt.Println("服务端拒绝连接")
return
}
fmt.Println("已连接，开始发 HTTP 请求...")

// 5. 发一个最简单的 HTTP GET 请求
fmt.Fprintf(conn, "GET / HTTP/1.0\r\nHost: %s\r\n\r\n", host)

// 6. 打印目标网站返回的数据
io.Copy(os.Stdout, conn)
}
