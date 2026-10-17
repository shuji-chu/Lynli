p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    content = f.read()

old = '''                    val suburl = subData?.subscribe_url ?: ""
                    val planName = subData?.plan?.name ?: "VPN"
                    if (suburl.isEmpty()) {
                        _log("startClash 无 sub_url，触发 performSubscrite")
                        performSubscrite()
                        return@launch
                    }'''

new = '''                    var suburl = subData?.subscribe_url ?: ""
                    var planName = subData?.plan?.name ?: "VPN"
                    if (suburl.isEmpty()) {
                        _log("startClash 无 sub_url，协程内主动拉取")
                        val apiServiceApp = ApiClient.retrofit.create(ApiService::class.java)
                        val resp = try { apiServiceApp.getSubscribe(PreferenceManager.loginauthData) } catch (_: Exception) { null }
                        if (resp != null && resp.isSuccessful && resp.body()?.data != null) {
                            subData = resp.body()?.data
                            suburl = subData?.subscribe_url ?: ""
                            planName = subData?.plan?.name ?: "VPN"
                        }
                    }
                    if (suburl.isEmpty()) {
                        _log("startClash 获取节点配置失败，抛出异常")
                        throw Exception("获取节点配置失败，请检查网络")
                    }'''

if old in content:
    content = content.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)
    print("SUCCESS: 已修复拉取失败卡死问题")
else:
    print("ERROR: 找不到目标代码块")
