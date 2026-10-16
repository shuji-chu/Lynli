package com.github.kr328.clash.remote

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.log.Log
import java.util.*
import com.github.kr328.clash.design.state.ConnectionState

class Broadcasts(private val context: Application) {
    interface Observer {
        fun onConnectionStateChanged(state: ConnectionState) {}
        fun onServiceRecreated()
        fun onStarted()
        fun onStopped(cause: String?)
        fun onProfileChanged()
        fun onProfileUpdateCompleted(uuid: UUID?)
        fun onProfileUpdateFailed(uuid: UUID?, reason: String?)
        fun onProfileLoaded()
    }

    var clashRunning: Boolean = false

    /** 连接状态机（大厂模型） */
    var connectionState: ConnectionState = ConnectionState.Idle
        private set

    // 锁定窗口：用户点击后 N ms 内不接受广播覆盖（防止按钮抖动）
    private var stateLockedUntil: Long = 0

    fun setConnectionState(state: ConnectionState, lockMs: Long = 0) {
        if (lockMs > 0) stateLockedUntil = System.currentTimeMillis() + lockMs
        if (connectionState == state) return
        connectionState = state
        clashRunning = state.isRunning
        receivers.forEach {
            try { it.onConnectionStateChanged(state) } catch (_: Throwable) {}
        }
    }

    private fun isStateLocked(): Boolean =
        System.currentTimeMillis() < stateLockedUntil

    private var registered = false
    private val receivers = mutableListOf<Observer>()
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` != context?.packageName)
                return

            when (intent?.action) {
                Intents.ACTION_SERVICE_RECREATED -> {
                    clashRunning = false

                    receivers.forEach {
                        it.onServiceRecreated()
                    }
                }
                Intents.ACTION_CLASH_STARTED -> {
                    clashRunning = true
                    setConnectionState(ConnectionState.Connected)

                    receivers.forEach {
                        it.onStarted()
                    }
                }
                Intents.ACTION_CLASH_STOPPED -> {
                    clashRunning = false
                    setConnectionState(ConnectionState.Idle)

                    receivers.forEach {
                        it.onStopped(intent.getStringExtra(Intents.EXTRA_STOP_REASON))
                    }
                }
                Intents.ACTION_PROFILE_CHANGED ->
                    receivers.forEach {
                        it.onProfileChanged()
                    }
                Intents.ACTION_PROFILE_UPDATE_COMPLETED ->
                    receivers.forEach {
                        it.onProfileUpdateCompleted(
                            UUID.fromString(intent.getStringExtra(Intents.EXTRA_UUID)))
                    }
                Intents.ACTION_PROFILE_UPDATE_FAILED ->
                    receivers.forEach {
                        it.onProfileUpdateFailed(
                            UUID.fromString(intent.getStringExtra(Intents.EXTRA_UUID)),
                            intent.getStringExtra(Intents.EXTRA_FAIL_REASON))
                    }
                Intents.ACTION_PROFILE_LOADED -> {
                    receivers.forEach {
                        it.onProfileLoaded()
                    }
                }
            }
        }
    }

    fun refreshRunningState() {
        if (isStateLocked()) return   // 锁定期内不覆盖用户意图
        val running = StatusClient(context).currentProfile() != null
        clashRunning = running
        if (connectionState != ConnectionState.Connecting &&
            connectionState != ConnectionState.Disconnecting) {
            setConnectionState(if (running) ConnectionState.Connected else ConnectionState.Idle)
        }
    }

    fun addObserver(observer: Observer) {
        receivers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        receivers.remove(observer)
    }

    fun register() {
        if (registered)
            return

        try {
            val filter = IntentFilter().apply {
                addAction(Intents.ACTION_SERVICE_RECREATED)
                addAction(Intents.ACTION_CLASH_STARTED)
                addAction(Intents.ACTION_CLASH_STOPPED)
                addAction(Intents.ACTION_PROFILE_CHANGED)
                addAction(Intents.ACTION_PROFILE_UPDATE_COMPLETED)
                addAction(Intents.ACTION_PROFILE_UPDATE_FAILED)
                addAction(Intents.ACTION_PROFILE_LOADED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, filter)
            }

            registered = true

            clashRunning = StatusClient(context).currentProfile() != null
            connectionState = if (clashRunning) ConnectionState.Connected else ConnectionState.Idle
        } catch (e: Exception) {
            Log.w("Register global receiver: $e", e)
        }
    }

    fun unregister() {
        if (!registered)
            return

        try {
            context.unregisterReceiver(broadcastReceiver)
            registered = false

            clashRunning = false
        } catch (e: Exception) {
            Log.w("Unregister global receiver: $e", e)
        }
    }
}
