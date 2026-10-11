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

    private const val OWNER = "shuji-chu"
    private const val REPO = "Lynli"

    fun check(activity: Activity) {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                if (conn.responseCode != 200) return@Thread
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val latestTag = json.optString("tag_name").trimStart('v', 'V')
                val assets = json.optJSONArray("assets") ?: return@Thread
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val name = assets.getJSONObject(i).optString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = assets.getJSONObject(i).optString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isNullOrEmpty()) return@Thread

                val currentVersion = activity.packageManager
                    .getPackageInfo(activity.packageName, 0).versionName
                    .trimStart('v', 'V')

                if (compareVersion(latestTag, currentVersion) <= 0) return@Thread

                Handler(Looper.getMainLooper()).post {
                    AlertDialog.Builder(activity)
                        .setTitle("发现新版本")
                        .setMessage("当前：$currentVersion\n最新：$latestTag")
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
