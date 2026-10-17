p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()
old = '''                        val client = okhttp3.OkHttpClient.Builder().connectTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS).readTimeout(2000, java.util.concurrent.TimeUnit.MILLISECONDS).callTimeout(3000, java.util.concurrent.TimeUnit.MILLISECONDS).build()'''
new = '''                        val client = com.github.kr328.clash.design.network.ApiClient.okHttpClient'''
if old in c:
    c = c.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(c)
    print("SUCCESS: 已替换为 ApiClient 网络栈")
else:
    print("ERROR: 找不到目标代码")
