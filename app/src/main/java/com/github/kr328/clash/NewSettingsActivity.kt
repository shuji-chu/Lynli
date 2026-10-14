package com.github.kr328.clash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager

class NewSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_new)

        // 返回
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        // 版本号
        try {
            val v = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
            findViewById<TextView>(R.id.tv_app_version).text = v
            findViewById<TextView>(R.id.tv_version_tag).text = v
        } catch (_: Throwable) {}

        // 外观 - 白屏
        findViewById<View>(R.id.item_theme_white)?.setOnClickListener {
            PreferenceManager.darkMode = "light"
            updateThemeTicks(0)
            Toast.makeText(this, "已切换白屏", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.item_theme_dark)?.setOnClickListener {
            PreferenceManager.darkMode = "dark"
            updateThemeTicks(1)
            Toast.makeText(this, "已切换黑屏", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.item_theme_auto)?.setOnClickListener {
            PreferenceManager.darkMode = "auto"
            updateThemeTicks(2)
            Toast.makeText(this, "已跟随系统", Toast.LENGTH_SHORT).show()
        }
        // 初始状态
        val cur = PreferenceManager.darkMode ?: "light"
        updateThemeTicks(when (cur) { "dark" -> 1; "auto" -> 2; else -> 0 })

        // 分应用代理
        findViewById<View>(R.id.item_per_app_proxy)?.setOnClickListener {
            try { startActivity(Intent(this, NetworkSettingsActivity::class.java)) } catch (_: Throwable) {}
        }

        // 代理模式
        findViewById<View>(R.id.item_proxy_mode)?.setOnClickListener {
            startActivity(Intent(this, ProxyActivity::class.java))
        }

        // 通知开关 - 连接状态
        val swConn = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_notify_conn)
        swConn.isChecked = PreferenceManager.notifyConn
        findViewById<View>(R.id.item_notify_conn)?.setOnClickListener {
            swConn.isChecked = !swConn.isChecked
            PreferenceManager.notifyConn = swConn.isChecked
        }

        // 通知开关 - 消息推送
        val swMsg = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_notify_msg)
        swMsg.isChecked = PreferenceManager.notifyMsg
        findViewById<View>(R.id.item_notify_msg)?.setOnClickListener {
            swMsg.isChecked = !swMsg.isChecked
            PreferenceManager.notifyMsg = swMsg.isChecked
        }

        // 检查更新
        findViewById<View>(R.id.item_check_update)?.setOnClickListener {
            UpdateChecker.check(this)
        }

        // 清除缓存
        findViewById<View>(R.id.item_clear_cache)?.setOnClickListener {
            try {
                cacheDir.deleteRecursively()
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.tv_cache_size).text = "0 MB"
            } catch (_: Throwable) {}
        }
        // 显示缓存大小
        try {
            val size = cacheDir.walkBottomUp().filter { it.isFile }.map { it.length() }.sum()
            findViewById<TextView>(R.id.tv_cache_size).text = "%.1f MB".format(size / 1024.0 / 1024.0)
        } catch (_: Throwable) {}

        // 隐私政策
        findViewById<View>(R.id.item_privacy)?.setOnClickListener {
            startActivity(Intent(this, TextPageActivity::class.java).apply {
                putExtra(TextPageActivity.EXTRA_TITLE, "隐私政策")
                putExtra(TextPageActivity.EXTRA_CONTENT, LegalTexts.PRIVACY)
            })
        }
        // 用户协议
        findViewById<View>(R.id.item_terms)?.setOnClickListener {
            startActivity(Intent(this, TextPageActivity::class.java).apply {
                putExtra(TextPageActivity.EXTRA_TITLE, "用户协议")
                putExtra(TextPageActivity.EXTRA_CONTENT, LegalTexts.TERMS)
            })
        }
        // 官网
        findViewById<View>(R.id.item_official)?.setOnClickListener {
            startActivity(Intent(this, H5WebActivity::class.java).apply {
                putExtra("url", "https://yisoujiasu.pages.dev")
            })
        }
        // 关于
        findViewById<View>(R.id.item_about)?.setOnClickListener {
            Toast.makeText(this, "Lynli · 全球加速", Toast.LENGTH_LONG).show()
        }

        // 退出登录
        findViewById<View>(R.id.item_logout)?.setOnClickListener {
            PreferenceManager.isLoginin = false
            PreferenceManager.loginToken = ""
            PreferenceManager.loginauthData = ""
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateThemeTicks(which: Int) {
        findViewById<View>(R.id.tick_white).visibility = if (which == 0) View.VISIBLE else View.GONE
        findViewById<View>(R.id.tick_dark).visibility = if (which == 1) View.VISIBLE else View.GONE
        findViewById<View>(R.id.tick_auto).visibility = if (which == 2) View.VISIBLE else View.GONE
    }
}
