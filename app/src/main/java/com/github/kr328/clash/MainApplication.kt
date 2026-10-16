package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.compat.currentProcessName
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.util.PreferenceManagerServer
import com.github.kr328.clash.service.util.sendServiceRecreated
import com.github.kr328.clash.util.clashDir
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


@Suppress("unused")
class MainApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        PreferenceManagerServer.init(this)
        PreferenceManager.init(this)

        // 最先初始化 AppContext（供 ApiClient 用）
        try { com.github.kr328.clash.design.util.AppContext.init(this) } catch (_: Throwable) {}

        Global.init(this)

    }

    override fun onCreate() {
        super.onCreate()
        try {
            val mode = com.github.kr328.clash.design.PreferenceManager.darkMode
            val nightMode = when (mode) {
                "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                "auto" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
        } catch (_: Throwable) {}

        val __def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { __t, __e ->
            try {
                val __dir = File(getExternalFilesDir(null), "crash")
                __dir.mkdirs()
                File(__dir, "c_${System.currentTimeMillis()}.txt").writeText(
                    "TIME: ${java.util.Date()}\nPROC: ${currentProcessName}\nTHREAD: ${__t.name}\n\n${__e.stackTraceToString()}"
                )
            } catch (_: Exception) {}

            // 网络类异常全部静默，不闪退
            val __n = __e.javaClass.name
            val __m = (__e.message ?: "").lowercase()
            val __netCrash = __n.contains("IOException") || __n.contains("Timeout") ||
                             __n.contains("SocketException") || __n.contains("UnknownHost") ||
                             __n.contains("ConnectException") || __n.contains("Retrofit") ||
                             __m.contains("unable to resolve host") || __m.contains("timeout") ||
                             __m.contains("failed to connect") || __m.contains("connection")
            if (__netCrash) {
                Log.d("LynliCrash: swallowed net error: " + __n + " / " + __e.message)
                return@setDefaultUncaughtExceptionHandler
            }

            // 其他非主线程崩溃：记录日志，不闪退
            if (__t != android.os.Looper.getMainLooper().thread) {
                Log.d("LynliCrash: non-main thread crash: " + __e.message)
                return@setDefaultUncaughtExceptionHandler
            }

            // 主线程崩溃：走系统默认（闪退），日志已保留
            __def?.uncaughtException(__t, __e)
        }

        val processName = currentProcessName
        extractGeoFiles()

        Log.d("Process $processName started")

        if (processName == packageName) {
            Remote.launch()
        } else {
            sendServiceRecreated()
        }
    }

    private fun extractGeoFiles() {
        clashDir.mkdirs();

        val geoipFile = File(clashDir, "geoip.metadb")
        if(!geoipFile.exists()) {
            FileOutputStream(geoipFile).use {
                assets.open("geoip.metadb").copyTo(it);
            }
        }

        val geositeFile = File(clashDir, "geosite.dat")
        if(!geositeFile.exists()) {
            FileOutputStream(geositeFile).use {
                assets.open("geosite.dat").copyTo(it);
            }
        }
        
        val ASNFile = File(clashDir, "ASN.mmdb")
        if(!ASNFile.exists()) {
            FileOutputStream(ASNFile).use {
                assets.open("ASN.mmdb").copyTo(it);
            }
        }
    }

    fun finalize() {
        Global.destroy()
    }
}