p = "app/src/main/java/com/github/kr328/clash/MainActivity.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

old = '''            LoadingDialog.show(this@MainActivity, "正在请求节点数据...")
            launch {'''
new = '''            LoadingDialog.show(this@MainActivity, "正在请求节点数据...")
            launch(Dispatchers.IO) {'''

if old in c:
    c = c.replace(old, new)
    with open(p, "w", encoding="utf-8") as f:
        f.write(c)
    print("SUCCESS: 已切换至 IO 线程，解决 UI 卡死")
else:
    print("ERROR: 找不到目标代码")
