package com.github.kr328.clash

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.safeApiRequestCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_wallet)

        val etName = findViewById<EditText>(R.id.et_real_name)
        val etUsdt = findViewById<EditText>(R.id.et_usdt)
        val etAlipay = findViewById<EditText>(R.id.et_alipay)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_goto_withdraw).setOnClickListener {
            startActivity(android.content.Intent(this, WithdrawActivity::class.java))
        }

        // 拉取已绑信息
        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            val auth = PreferenceManager.loginauthData ?: ""
            val r = safeApiRequestCall { api.getWalletInfo(auth) }
            val d = r?.body()?.data
            withContext(Dispatchers.Main) {
                if (d != null) {
                    etName.setText(d.real_name ?: "")
                    etUsdt.setText(d.usdt_addr ?: "")
                    etAlipay.setText(d.alipay ?: "")
                    when (d.wallet_status ?: 0) {
                        1 -> { tvStatus.text = "✅ 审核已通过"; tvStatus.setTextColor(0xFF0A9F52.toInt()); tvStatus.visibility = View.VISIBLE }
                        0 -> if (!(d.real_name.isNullOrEmpty())) {
                            tvStatus.text = "⏳ 待审核"; tvStatus.setTextColor(0xFFD97706.toInt()); tvStatus.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        // 提交
        findViewById<View>(R.id.btn_submit).setOnClickListener {
            val rn = etName.text.toString().trim()
            val us = etUsdt.text.toString().trim()
            val al = etAlipay.text.toString().trim()
            if (rn.isEmpty()) { Toast.makeText(this, "请填写真实姓名", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (us.isEmpty() && al.isEmpty()) { Toast.makeText(this, "至少填一个收款方式", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            CoroutineScope(Dispatchers.IO).launch {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val r = safeApiRequestCall {
                    api.submitWallet(auth, mapOf("real_name" to rn, "usdt_addr" to us, "alipay" to al))
                }
                val ok = r?.body()?.status == "success"
                val msg = r?.body()?.message ?: "提交失败"
                withContext(Dispatchers.Main) {
                    if (ok) {
                        Toast.makeText(this@WalletActivity, "已提交，等待审核", Toast.LENGTH_LONG).show()
                        tvStatus.text = "⏳ 待审核"
                        tvStatus.setTextColor(0xFFD97706.toInt())
                        tvStatus.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this@WalletActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
