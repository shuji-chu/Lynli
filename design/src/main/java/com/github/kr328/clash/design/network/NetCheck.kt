package com.github.kr328.clash.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetCheck {
    fun isOnline(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(n) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) { true }
    }
}
