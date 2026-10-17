p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    content = f.read()

old = '''                    var newULR = suburl
                    if (ApiClientConfig.ConfigNodeURL.length > 3) {
                        val encodeURL = java.net.URLEncoder.encode(suburl, "utf-8").replace("?", "%3F")
                        newULR = ApiClientConfig.ConfigNodeURL + encodeURL
                    }
                    _log("startClash 创建 profile name=" + planName)
                    val uuid2 = withProfile {
                        create(Profile.Type.Url, planName).also {
                            patch(it, planName, newULR, 0)
                        }
                    }'''

new = '''                    var newULR = suburl
                    if (ApiClientConfig.ConfigNodeURL.length > 3) {
                        val encodeURL = java.net.URLEncoder.encode(suburl, "utf-8").replace("?", "%3F")
                        newULR = ApiClientConfig.ConfigNodeURL + encodeURL
                    }
                    _log("startClash 正在下载订阅: " + newULR)
                    val saveFile = java.io.File(filesDir, "profile_" + System.currentTimeMillis() + ".yaml")
                    withContext(Dispatchers.IO) {
                        val client = okhttp3.OkHttpClient.Builder().build()
                        val request = okhttp3.Request.Builder().url(newULR).build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) throw Exception("下载订阅失败: HTTP " + response.code)
                        val yamlContent = response.body?.string() ?: ""
                        if (yamlContent.isEmpty()) throw Exception("下载订阅为空")
                        saveFile.writeText(yamlContent)
                    }
                    _log("startClash 订阅已保存至 " + saveFile.absolutePath)
                    _log("startClash 创建 profile name=" + planName)
                    val uuid2 = withProfile {
                        create(Profile.Type.File, planName).also {
                            patch(it, planName, saveFile.absolutePath, 0)
                        }
                    }'''

if old not in content:
    print("ERROR: 找不到要替换的代码块")
else:
    content = content.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)
    print("SUCCESS: 替换成功")
