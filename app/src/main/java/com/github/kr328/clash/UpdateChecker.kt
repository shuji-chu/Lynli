package com.github.kr328.clash

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val VERSION_URL = "https://dl.yisoujiasu.de5.net/version.json"

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

                val notesList = mutableListOf<String>()
                val arr = json.optJSONArray("notes")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val t = arr.optString(i)
                        if (t.isNotBlank()) notesList.add(t)
                    }
                } else {
                    val s = json.optString("notes")
                    if (s.isNotBlank()) notesList.add(s)
                }

                if (apkUrl.isEmpty() || latestTag.isEmpty()) return@Thread

                val currentVersion = activity.packageManager
                    .getPackageInfo(activity.packageName, 0).versionName
                    .trimStart('v', 'V')

                if (compareVersion(latestTag, currentVersion) <= 0) return@Thread

                Handler(Looper.getMainLooper()).post {
                    showUpdateDialog(activity, currentVersion, latestTag, notesList, apkUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun showUpdateDialog(
        activity: Activity,
        currentVersion: String,
        latestTag: String,
        notes: List<String>,
        apkUrl: String
    ) {
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_update, null)

        view.findViewById<TextView>(R.id.tvVersion).text = "v$latestTag"
        view.findViewById<TextView>(R.id.tvDesc).text = "版本号：$currentVersion  →  $latestTag"

        val llNotes = view.findViewById<LinearLayout>(R.id.llNotes)
        val list = if (notes.isEmpty()) listOf("优化使用体验，修复已知问题") else notes
        for (line in list) {
            val tv = TextView(activity)
            tv.text = "•  $line"
            tv.textSize = 14f
            tv.setTextColor(Color.parseColor("#333333"))
            tv.setLineSpacing(8f, 1.0f)
            tv.setPadding(0, 8, 0, 8)
            llNotes.addView(tv)
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.btnUpdate).setOnClickListener {
            dialog.dismiss()
            downloadAndInstall(activity, apkUrl, latestTag)
        }
        view.findViewById<TextView>(R.id.btnLater).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun downloadAndInstall(activity: Activity, url: String, version: String) {
        val dir = File(activity.getExternalFilesDir(null), "update")
        if (!dir.exists()) dir.mkdirs()
        val apkFile = File(dir, "Lynli-$version.apk")
        if (apkFile.exists()) apkFile.delete()

        val progress = ProgressDialog(activity).apply {
            setTitle("正在下载更新")
            setMessage("正在连接…")
            setCancelable(false)
            show()
        }

        Thread {
            var errMsg: String? = null
            try {
                val conn = openWithRedirect(url)
                val code = conn.responseCode
                if (code != 200) throw Exception("HTTP $code")
                val total = conn.contentLength.toLong()
                val input = conn.inputStream
                val output = FileOutputStream(apkFile)

                val buf = ByteArray(16384)
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
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            progress.setMessage("${sum / 1024 / 1024} MB")
                        }
                    }
                }
                output.flush()
                output.close()
                input.close()
            } catch (e: Exception) {
                e.printStackTrace()
                errMsg = e.message ?: e.toString()
            }

            Handler(Looper.getMainLooper()).post {
                try { progress.dismiss() } catch (_: Exception) {}
                if (errMsg == null) {
                    installApk(activity, apkFile)
                } else {
                    AlertDialog.Builder(activity)
                        .setTitle("下载失败")
                        .setMessage(errMsg)
                        .setPositiveButton("知道了", null)
                        .show()
                }
            }
        }.start()
    }

    private fun openWithRedirect(url: String, maxRedirects: Int = 8): HttpURLConnection {
        var current = url
        var hops = 0
        while (true) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) Lynli")
            conn.setRequestProperty("Accept", "*/*")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc.isNullOrEmpty() || hops >= maxRedirects) {
                    throw Exception("重定向异常 (HTTP $code)")
                }
                current = if (loc.startsWith("http")) loc
                          else URL(URL(current), loc).toString()
                hops++
                continue
            }
            return conn
        }
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
        // 安装界面弹出后，杀掉当前进程，用户装完新版本重新打开就是新版
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 2000)
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
