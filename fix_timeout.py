import re

# 1. 改 ApiClient.kt 的所有超时
p1 = "design/src/main/java/com/github/kr328/clash/design/network/ApiClient.kt"
with open(p1, "r", encoding="utf-8") as f:
    c1 = f.read()

# 全局替换
c1 = c1.replace(".connectTimeout(15, TimeUnit.SECONDS)", ".connectTimeout(2000, TimeUnit.MILLISECONDS)")
c1 = c1.replace(".readTimeout(30, TimeUnit.SECONDS)", ".readTimeout(3000, TimeUnit.MILLISECONDS)")
c1 = c1.replace(".writeTimeout(15, TimeUnit.SECONDS)", ".writeTimeout(2000, TimeUnit.MILLISECONDS)")
c1 = c1.replace(".connectTimeout(6, TimeUnit.SECONDS)", ".connectTimeout(2000, TimeUnit.MILLISECONDS)")
c1 = c1.replace(".readTimeout(6, TimeUnit.SECONDS)", ".readTimeout(3000, TimeUnit.MILLISECONDS)")

# 给所有 OkHttpClient.Builder() 加全局 callTimeout(5000 ms)
c1 = c1.replace("OkHttpClient.Builder()\n", "OkHttpClient.Builder()\n        .callTimeout(5000, TimeUnit.MILLISECONDS)\n")

with open(p1, "w", encoding="utf-8") as f:
    f.write(c1)
print("SUCCESS: ApiClient 超时已改毫秒级")

# 2. 改 MainActivity 里下载订阅的超时
p2 = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p2, "r", encoding="utf-8") as f:
    c2 = f.read()

old = "val client = okhttp3.OkHttpClient.Builder().build()"
new = "val client = okhttp3.OkHttpClient.Builder().connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS).readTimeout(3, java.util.concurrent.TimeUnit.SECONDS).callTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()"

if old in c2:
    c2 = c2.replace(old, new)
    with open(p2, "w", encoding="utf-8") as f:
        f.write(c2)
    print("SUCCESS: MainActivity 下载超时已改")
else:
    print("ERROR: MainActivity 未找到 client")

