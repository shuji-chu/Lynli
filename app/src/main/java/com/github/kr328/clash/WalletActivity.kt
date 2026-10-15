package com.github.kr328.clash

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager

class WalletActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_wallet)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        // 提交按钮先占位
        findViewById<View>(R.id.btn_submit).setOnClickListener {
            android.widget.Toast.makeText(this, "钱包提交功能开发中", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
