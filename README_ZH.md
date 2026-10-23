# UUVPN（Lynli）项目中文说明

## 一句话总结

本项目是基于开源 Clash Meta for Android（CMFA）二次开发的 Android 商业机场/加速器客户端，内部代号 UUVPN，仓库名 Lynli。在原客户端基础上接入自有后端 API，实现注册推广、订阅节点下发、支付、在线客服、首页横幅等商业化功能。

## 项目性质

- 类型：Android 应用（Kotlin + Go 内核）
- 基础：CMFA / Clash Meta for Android 魔改分支
- 定位：付费 VPN / 机场客户端
- 最低系统：Android 5.0+（推荐 7.0+）
- 架构支持：armeabi-v7a、arm64-v8a、x86、x86_64
- 语言：Kotlin 为主，含 Go 子模块内核

## 模块职责

| 模块 | 作用 |
| --- | --- |
| app | 主应用模块：Activity/Fragment、页面导航、后端 API 调用、设置页、打包配置 |
| core | 核心逻辑：配置解析、Clash 配置生成、状态管理、数据模型 |
| service | VPN 服务、代理服务、前台服务、AIDL 通信 |
| design | UI 设计系统、主题、通用组件、弹窗、列表项 |
| common | 公共工具类、常量、扩展函数、日志 |
| hideapi | 编译期访问 Android 隐藏 API，避免反射 |

## 技术要点

- 使用 kr328/golang Gradle 插件编译 Go 内核
- 使用 kaidl 生成 AIDL 接口，rikkax.multiprocess 做多进程通信
- 使用 hideapi 在编译期访问隐藏 API
- 构建时自动下载 MetaCubeX 规则库：geoip.metadb、geosite.dat、ASN.mmdb
- 网络层：Retrofit2 + OkHttp + Gson
- 二维码：zxing core
- 依赖版本集中在 settings.gradle.kts 的 versionCatalogs 中管理

## 后端配置字段

应用运行时会从后端拉取一份配置 JSON，字段含义如下：

| 字段 | 含义 |
| --- | --- |
| baseURL | 主 API 地址，所有主要接口基于此拼接 |
| baseDYURL | 默认测试节点地址，用于获取默认 VPN 节点 |
| mainregisterURL | 注册/推广页地址，通常带邀请码参数 |
| paymentURL | 支付跳转地址 |
| telegramurl | Telegram 群/频道地址 |
| kefuurl | 在线客服页面地址 |
| websiteURL | 官网地址 |
| crisptoken | Crisp 在线客服 SDK 的 Token |
| banners | 首页横幅图片地址数组 |
| message | 接口返回状态信息 |
| code | 接口返回状态码，1 表示成功 |

## 构建步骤

1. 初始化子模块：

   git submodule update --init --recursive

2. 安装 OpenJDK 11、Android SDK、CMake、Golang。

3. 项目根目录创建 local.properties：

   sdk.dir=/path/to/android-sdk

4. 项目根目录创建 signing.properties：

   keystore.path=/path/to/keystore/file
   keystore.password=密钥库密码
   key.alias=密钥别名
   key.password=密钥密码

5. 构建：

   ./gradlew app:assembleMeta-AlphaRelease

## 常见修改点

| 需求 | 修改位置 |
| --- | --- |
| 改后端 API 地址 | 后端配置 JSON 的 baseURL / baseDYURL，或 app 模块读取配置的代码 |
| 改 App 名称 | app/src/main/res/values/strings.xml 中的 app_name |
| 改包名 | app/build.gradle.kts 中的 applicationId 与 AndroidManifest.xml |
| 改图标 | app/src/main/res/mipmap-* 下的 ic_launcher |
| 改签名 | release.keystore 与 signing.properties |
| 改规则库下载地址 | app/build.gradle.kts 中 downloadGeoFiles 任务的 geoFilesUrls |
| 改依赖版本 | settings.gradle.kts 中的 versionCatalogs |

构建产物：app/build/outputs/apk/

## 重要提醒（安全）

仓库中直接包含以下敏感文件/信息，属于安全隐患，不建议在公开仓库保留：

- release.keystore：正式签名密钥，泄露后任何人可签同名应用
- local.properties：本机 SDK 路径，可能含本地信息
- README.md 中的测试账号与密码
- crisptoken：客服系统 Token

建议：加入 .gitignore、从仓库历史移除，并更换已泄露的密钥与 Token。

## 文档索引

| 文件 | 说明 |
| --- | --- |
| README.md | 原版英文说明与后端配置示例 |
| README_ZH.md | 本文件，中文项目说明 |
| 服务器配置+Kotlin-Android 文档教程.docx | 服务器与 Kotlin/Android 配置教程 |
| CONTRIBUTING.md | 贡献说明 |
| PRIVACY_POLICY.md | 隐私政策 |
| NOTICE | 第三方许可声明 |
| LICENSE | 许可证 |
