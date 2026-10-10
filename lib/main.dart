import 'package:flutter/material.dart';

void main() => runApp(const YSApp());

class YSApp extends StatelessWidget {
  const YSApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'YS',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, primarySwatch: Colors.blue),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _index = 0;
  bool _connected = false;

  @override
  Widget build(BuildContext context) {
    final pages = [_homePage(), _planPage(), _mePage()];
    return Scaffold(
      body: pages[_index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.bolt), label: '主页'),
          NavigationDestination(icon: Icon(Icons.card_membership), label: '套餐'),
          NavigationDestination(icon: Icon(Icons.person), label: '我的'),
        ],
      ),
    );
  }

  Widget _homePage() {
    return SafeArea(
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Icon(_connected ? Icons.shield : Icons.shield_outlined,
                    color: _connected ? Colors.green : Colors.grey),
                const SizedBox(width: 8),
                Text(_connected ? 'V3.0.3' : '未连接'),
                const Spacer(),
                const Icon(Icons.notifications_none),
                const SizedBox(width: 12),
                const Icon(Icons.menu),
              ],
            ),
          ),
          Expanded(
            child: Container(
              color: const Color(0xFFE8F0E8),
              child: Stack(
                children: [
                  const Center(
                    child: Text('🗾',
                        style: TextStyle(fontSize: 200)),
                  ),
                  if (_connected)
                    const Positioned(
                      top: 150, left: 130,
                      child: Icon(Icons.circle, color: Colors.green, size: 20),
                    ),
                ],
              ),
            ),
          ),
          Container(
            margin: const EdgeInsets.all(12),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 8)],
            ),
            child: Column(
              children: [
                Row(
                  children: [
                    const Icon(Icons.calendar_today, size: 16),
                    const SizedBox(width: 8),
                    const Text('到期时间 '),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.blue.shade50,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text('2026-11-02',
                          style: TextStyle(color: Colors.blue)),
                    ),
                    const Spacer(),
                    const Icon(Icons.cloud, size: 16),
                    const SizedBox(width: 4),
                    const Text('已用 6.36 GB / 60 GB'),
                  ],
                ),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: 0.11,
                    minHeight: 6,
                    backgroundColor: Colors.grey.shade200,
                  ),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () {},
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(double.infinity, 40),
                  ),
                  child: const Text('续费套餐'),
                ),
                const SizedBox(height: 8),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.public),
                  title: const Text('节点选择'),
                  subtitle: const Text('【亚洲】日本02 | Vless'),
                  trailing: const Text('38ms', style: TextStyle(color: Colors.green)),
                  onTap: () {},
                ),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () {},
                        child: const Text('智能分流'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () {},
                        child: const Text('全局代理'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: () => setState(() => _connected = !_connected),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _connected ? Colors.red : Colors.blue,
                      foregroundColor: Colors.white,
                    ),
                    child: Text(_connected ? '断开连接' : '连接',
                        style: const TextStyle(fontSize: 16)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _planPage() {
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text('选择套餐', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          _planCard('月付', '￥15', '30天 / 100GB'),
          _planCard('季付', '￥40', '90天 / 300GB'),
          _planCard('年付', '￥150', '365天 / 1TB'),
        ],
      ),
    );
  }

  Widget _planCard(String name, String price, String desc) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        title: Text(name),
        subtitle: Text(desc),
        trailing: Text(price,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.blue)),
        onTap: () {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('选择 $name')),
          );
        },
      ),
    );
  }

  Widget _mePage() {
    return SafeArea(
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const CircleAvatar(radius: 40, child: Icon(Icons.person, size: 40)),
            const SizedBox(height: 16),
            const Text('未登录'),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: () {}, child: const Text('登录 / 注册')),
          ],
        ),
      ),
    );
  }
}
