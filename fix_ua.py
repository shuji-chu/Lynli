p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    content = f.read()

old = '''                        val request = okhttp3.Request.Builder().url(newULR).build()'''
new = '''                        val request = okhttp3.Request.Builder().url(newULR)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()'''

if old not in content:
    print("ERROR: 找不到请求代码")
else:
    content = content.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)
    print("SUCCESS: 已加伪装 UA")
