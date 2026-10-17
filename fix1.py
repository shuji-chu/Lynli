p = "design/src/main/java/com/github/kr328/clash/design/network/ApiClient.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()
c = c.replace("private val okHttpClient = OkHttpClient.Builder()", "val okHttpClient = OkHttpClient.Builder()")
with open(p, "w", encoding="utf-8") as f:
    f.write(c)
print("SUCCESS: ApiClient 已暴露")
