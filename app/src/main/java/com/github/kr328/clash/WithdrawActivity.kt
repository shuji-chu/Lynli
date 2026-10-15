package com.github.kr328.clash

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager

class WithdrawActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_withdraw)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_submit).setOnClickListener {
            android.widget.Toast.makeText(this, "提现功能开发中", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
