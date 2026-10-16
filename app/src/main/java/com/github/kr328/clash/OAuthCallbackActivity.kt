package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager

class OAuthCallbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        val token = data?.getQueryParameter("token") ?: ""
        val sid = data?.getQueryParameter("short_id") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "登录失败：未收到凭据", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PreferenceManager.init(this)
        PreferenceManager.loginauthData = token
        PreferenceManager.loginToken = token
        PreferenceManager.isLoginin = true
        try {
            val p = applicationContext.getSharedPreferences("lynli_oauth", 0)
            p.edit().putString("short_id", sid).apply()
        } catch (_: Throwable) {}

        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }
}
