package com.github.kr328.clash.remote

/**
 * 连接状态机（大厂模型）：
 *   Idle         — 已断开
 *   Connecting   — 正在建立（点连接后 → VPN 授权 → 起 service → 等内核 ready）
 *   Connected    — 已连通（内核启动广播到达）
 *   Disconnecting— 正在断开
 *   Failed       — 失败（授权被拒 / 内核异常 / 超时）
 */
enum class ConnectionState {
    Idle,
    Connecting,
    Connected,
    Disconnecting,
    Failed;

    val isBusy: Boolean get() = this == Connecting || this == Disconnecting
    val isRunning: Boolean get() = this == Connected || this == Connecting
}
