p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

old = '''                    _log("startClash 正在下载订阅: " + newULR)
                    val saveFile = java.io.File(filesDir, "profile_" + System.currentTimeMillis() + ".yaml")
                    withContext(Dispatchers.IO) {
                        val client = okhttp3.OkHttpClient.Builder().connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS).readTimeout(30, java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
                        val request = okhttp3.Request.Builder().url(newULR)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) throw Exception("下载订阅失败: HTTP " + response.code)
                        val yamlContent = response.body?.string() ?: ""
                        if (yamlContent.isEmpty()) throw Exception("下载订阅为空")
                        saveFile.writeText(yamlContent)
                    }'''

new = '''                    _log("startClash 本地生成节点配置（0网络请求）")
                    val saveFile = java.io.File(filesDir, "profile_" + System.currentTimeMillis() + ".yaml")
                    val userUUID = subData?.uuid ?: "a9ed8ff8-aa4c-463e-801e-21405abf8525"
                    val yamlText = "mixed-port: 7890\\nallow-lan: false\\nmode: rule\\nlog-level: warning\\nexternal-controller: 127.0.0.1:9090\\nproxies:\\n  - name: \\"易搜加速·美西\\"\\n    type: vless\\n    server: us1.yisoujiasu.de5.net\\n    port: 52610\\n    uuid: " + userUUID + "\\n    network: tcp\\n    udp: true\\n    tls: true\\n    flow: xtls-rprx-vision\\n    servername: www.microsoft.com\\n    reality-opts:\\n      public-key: 8ZeOiFx6AFBRhJjJ7-8Nwg2qHYhChQdEyigsRmOHol0\\n      short-id: b316cc48\\n    client-fingerprint: chrome\\nproxy-groups:\\n  - name: \\"🚀 节点选择\\"\\n    type: select\\n    proxies:\\n      - \\"易搜加速·美西\\"\\n      - DIRECT\\nrules:\\n  - GEOIP,CN,DIRECT\\n  - MATCH,🚀 节点选择\\n"
                    withContext(Dispatchers.IO) {
                        saveFile.writeText(yamlText)
                    }'''

if old in c:
    c = c.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(c)
    print("SUCCESS: 彻底移除网络下载，改为本地生成")
else:
    print("ERROR: 找不到旧代码")
