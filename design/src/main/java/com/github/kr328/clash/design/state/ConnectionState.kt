package com.github.kr328.clash.design.state

enum class ConnectionState {
    Idle,
    Connecting,
    Connected,
    Disconnecting,
    Failed;

    val isBusy: Boolean get() = this == Connecting || this == Disconnecting
    val isRunning: Boolean get() = this == Connected || this == Connecting
}
