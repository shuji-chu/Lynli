# 1. 修复 MainActivity，去掉 file:// 前缀（内核底层是用 File(source) 读的，不能带前缀）
p2 = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p2, "r", encoding="utf-8") as f:
    c2 = f.read()
old2 = '''patch(it, planName, "file://" + saveFile.absolutePath, 0)'''
new2 = '''patch(it, planName, saveFile.absolutePath, 0)'''
if old2 in c2:
    c2 = c2.replace(old2, new2)
    with open(p2, "w", encoding="utf-8") as f:
        f.write(c2)
    print("SUCCESS: MainActivity 已恢复纯路径")
else:
    print("ERROR: MainActivity 找不到目标")

# 2. 修复 ProfileProcessor，让 File 类型的 source 跳过 URL 格式校验
p1 = "service/src/main/java/com/github/kr328/clash/service/ProfileProcessor.kt"
with open(p1, "r", encoding="utf-8") as f:
    c1 = f.read()
old1 = '''            source.isNotEmpty() && scheme != "https" && scheme != "http" && scheme != "content" ->
                throw IllegalArgumentException("Unsupported url $source")'''
new1 = '''            source.isNotEmpty() && type != Profile.Type.File && scheme != "https" && scheme != "http" && scheme != "content" ->
                throw IllegalArgumentException("Unsupported url $source")'''
if old1 in c1:
    c1 = c1.replace(old1, new1)
    with open(p1, "w", encoding="utf-8") as f:
        f.write(c1)
    print("SUCCESS: ProfileProcessor 已放行本地文件")
else:
    print("ERROR: ProfileProcessor 找不到目标")
