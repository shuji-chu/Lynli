p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

# 1. 在开始点击连接时，直接暴力清理所有旧的 Profile 缓存
old_start = '''        // 重新查询（因为可能刚删了）
        val activeProfile2 = withProfile { queryActive() }
        if (activeProfile2 == null) {'''

new_start = '''        // 强制清理旧 Profile，防止读到之前的脏数据
        withProfile {
            val all = queryAll()
            all.forEach { delete(it.uuid) }
        }
        
        // 重新查询
        val activeProfile2 = withProfile { queryActive() }
        if (activeProfile2 == null) {'''

if old_start in c:
    c = c.replace(old_start, new_start)
    print("SUCCESS: 已添加自动清理旧配置逻辑")
else:
    print("ERROR: 找不到清理入口")

# 2. 修复 YAML 字符串拼接转义，改用 Kotlin 原始字符串
old_yaml = '''                    val yamlText = "mixed-port: 7890\\nallow-lan: false\\nmode: rule\\nlog-level: warning\\nexternal-controller: 127.0.0.1:9090\\nproxies:\\n  - name: \\"易搜加速·美西\\"\\n    type: vless\\n    server: us1.yisoujiasu.de5.net\\n    port: 52610\\n    uuid: " + userUUID + "\\n    network: tcp\\n    udp: true\\n    tls: true\\n    flow: xtls-rprx-vision\\n    servername: www.microsoft.com\\n    reality-opts:\\n      public-key: 8ZeOiFx6AFBRhJjJ7-8Nwg2qHYhChQdEyigsRmOHol0\\n      short-id: b316cc48\\n    client-fingerprint: chrome\\nproxy-groups:\\n  - name: \\"🚀 节点选择\\"\\n    type: select\\n    proxies:\\n      - \\"易搜加速·美西\\"\\n      - DIRECT\\nrules:\\n  - GEOIP,CN,DIRECT\\n  - MATCH,🚀 节点选择\\n"'''

new_yaml = '''                    val yamlText = """
                        mixed-port: 7890
                        allow-lan: false
                        mode: rule
                        log-level: warning
                        external-controller: 127.0.0.1:9090
                        proxies:
                          - name: "易搜加速·美西"
                            type: vless
                            server: us1.yisoujiasu.de5.net
                            port: 52610
                            uuid: $userUUID
                            network: tcp
                            udp: true
                            tls: true
                            flow: xtls-rprx-vision
                            servername: www.microsoft.com
                            reality-opts:
                              public-key: 8ZeOiFx6AFBRhJjJ7-8Nwg2qHYhChQdEyigsRmOHol0
                              short-id: b316cc48
                            client-fingerprint: chrome
                        proxy-groups:
                          - name: "🚀 节点选择"
                            type: select
                            proxies:
                              - "易搜加速·美西"
                              - DIRECT
                        rules:
                          - GEOIP,CN,DIRECT
                          - MATCH,🚀 节点选择
                    """.trimIndent()'''

if old_yaml in c:
    c = c.replace(old_yaml, new_yaml)
    print("SUCCESS: YAML 字符串已重构为原始字符串")
else:
    print("ERROR: 找不到 YAML 生成代码")

with open(p, "w", encoding="utf-8") as f:
    f.write(c)
