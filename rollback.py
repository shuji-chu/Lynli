# 1. 恢复 ApiClient 的超时为正常秒级
p1 = "design/src/main/java/com/github/kr328/clash/design/network/ApiClient.kt"
with open(p1, "r", encoding="utf-8") as f:
    c1 = f.read()
c1 = c1.replace(".callTimeout(5000, TimeUnit.MILLISECONDS)\n", "")
c1 = c1.replace(".connectTimeout(2000, TimeUnit.MILLISECONDS)", ".connectTimeout(15, TimeUnit.SECONDS)")
c1 = c1.replace(".readTimeout(3000, TimeUnit.MILLISECONDS)", ".readTimeout(30, TimeUnit.SECONDS)")
c1 = c1.replace(".writeTimeout(2000, TimeUnit.MILLISECONDS)", ".writeTimeout(15, TimeUnit.SECONDS)")
c1 = c1.replace("val okHttpClient = OkHttpClient.Builder()", "private val okHttpClient = OkHttpClient.Builder()")
with open(p1, "w", encoding="utf-8") as f:
    f.write(c1)

# 2. 恢复 MainActivity 下载的 client，去掉 ApiClient 引用
p2 = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p2, "r", encoding="utf-8") as f:
    c2 = f.read()
old = '''val client = com.github.kr328.clash.design.network.ApiClient.okHttpClient'''
new = '''val client = okhttp3.OkHttpClient.Builder().connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS).readTimeout(30, java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(true).build()'''
if old in c2:
    c2 = c2.replace(old, new)
    with open(p2, "w", encoding="utf-8") as f:
        f.write(c2)
    print("SUCCESS: MainActivity 下载已恢复正常超时")
else:
    print("ERROR: MainActivity 未找到 client")

print("SUCCESS: 超时回滚完成")
