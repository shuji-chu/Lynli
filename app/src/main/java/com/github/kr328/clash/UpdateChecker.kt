package com.github.kr328.clash

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val VERSION_URL = "https://yisoujiasu.pages.dev/version.json"

    fun check(activity: Activity) {
        Thread {
            try {
                val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                if (conn.responseCode != 200) return@Thread
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val latestTag = json.optString("version").trimStart('v', 'V')
                val apkUrl = json.optString("url")
                val notes = json.optString("notes")
                if (apkUrl.isEmpty() || latestTag.isEmpty()) return@Thread

                val currentVersion = activity.packageManager
                    .getPackageInfo(activity.packageName, 0).versionName
                    .trimStart('v', 'V')

                if (compareVersion(latestTag, currentVersion) <= 0) return@Thread

                Handler(Looper.getMainLooper()).post {
                    AlertDialog.Builder(activity)
                        .setTitle("发现新版本 v$latestTag")
                        .setMessage("当前版本：$currentVersion\n最新版本：$latestTag\n\n$notes")
                        .setCancelable(false)
                        .setPositiveButton("立即更新") { _, _ ->
                            downloadAndInstall(activity, apkUrl, latestTag)
                        }
                        .setNegativeButton("稍后", null)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun downloadAndInstall(activity: Activity, url: String, version: String) {
        val dir = File(activity.getExternalFilesDir(null), "update")
        if (!dir.exists()) dir.mkdirs()
        val apkFile = File(dir, "Lynli-$version.apk")

        val progress = ProgressDialog(activity).apply {
            setTitle("正在下载更新")
            setMessage("0%")
            setCancelable(false)
            show()
        }

        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true
                val total = conn.contentLength.toLong()
                val input = conn.inputStream
                val output = FileOutputStream(apkFile)

                val buf = ByteArray(8192)
                var read: Int
                var sum = 0L
                while (input.read(buf).also { read = it } > 0) {
                    output.write(buf, 0, read)
                    sum += read
                    if (total > 0) {
                        val pct = (sum * 100 / total).toInt()
                        Handler(Looper.getMainLooper()).post {
                            progress.setMessage("$pct%")
                        }
                    }
                }
                output.close()
                input.close()

                Handler(Looper.getMainLooper()).post {
                    progress.dismiss()
                    installApk(activity, apkFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    progress.dismiss()
                }
            }
        }.start()
    }

    private fun installApk(activity: Activity, apk: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apk
            )
        } else {
            Uri.fromFile(apk)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    private fun compareVersion(a: String, b: String): Int {
        val ap = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bp = b.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(ap.size, bp.size)
        for (i in 0 until len) {
            val x = ap.getOrElse(i) { 0 }
            val y = bp.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
