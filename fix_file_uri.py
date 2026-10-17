p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    content = f.read()

old = '''                        create(Profile.Type.File, planName).also {
                            patch(it, planName, saveFile.absolutePath, 0)
                        }'''

new = '''                        create(Profile.Type.File, planName).also {
                            patch(it, planName, "file://" + saveFile.absolutePath, 0)
                        }'''

if old in content:
    content = content.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)
    print("SUCCESS: 已修复 file:// 前缀")
else:
    print("ERROR: 找不到目标代码块")
