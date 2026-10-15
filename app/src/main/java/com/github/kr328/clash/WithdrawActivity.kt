package com.github.kr328.clash

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WithdrawActivity : AppCompatActivity() {

    private var walletStatus = 0
    private var usdtBound = false
    private var alipayBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_withdraw)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val rbAlipay = findViewById<RadioButton>(R.id.rb_alipay)
        val rbUsdt = findViewById<RadioButton>(R.id.rb_usdt)
        val tvAlipaySt = findViewById<TextView>(R.id.tv_alipay_status)
        val tvUsdtSt = findViewById<TextView>(R.id.tv_usdt_status)
        val rowAlipay = findViewById<LinearLayout>(R.id.row_alipay)
        val rowUsdt = findViewById<LinearLayout>(R.id.row_usdt)
        val etAmount = findViewById<EditText>(R.id.et_amount)
        val tvFee = findViewById<TextView>(R.id.tv_fee_preview)

        rowAlipay.setOnClickListener { rbAlipay.isChecked = true; rbUsdt.isChecked = false }
        rowUsdt.setOnClickListener { rbUsdt.isChecked = true; rbAlipay.isChecked = false }

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toDoubleOrNull() ?: 0.0
                if (v > 0) {
                    val fee = v * 0.08
                    val real = v - fee
                    tvFee.text = "手续费 ¥%.2f · 实到 ¥%.2f".format(fee, real)
                    tvFee.visibility = View.VISIBLE
                } else {
                    tvFee.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 拉钱包 + 提现记录
        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            val auth = PreferenceManager.loginauthData ?: ""
            val w = safeApiRequestCall { api.getWalletInfo(auth) }
            val wd = w?.body()?.data
            walletStatus = wd?.wallet_status ?: 0
            usdtBound = !(wd?.usdt_addr.isNullOrEmpty())
            alipayBound = !(wd?.alipay.isNullOrEmpty())

            withContext(Dispatchers.Main) {
                tvAlipaySt.text = if (alipayBound) "已绑定" else "未绑定"
                tvUsdtSt.text = if (usdtBound) "已绑定" else "未绑定"
                tvAlipaySt.setTextColor(if (alipayBound) 0xFF0A9F52.toInt() else 0xFF94A3B8.toInt())
                tvUsdtSt.setTextColor(if (usdtBound) 0xFF0A9F52.toInt() else 0xFF94A3B8.toInt())
                if (!alipayBound && !usdtBound) {
                    Toast.makeText(this@WithdrawActivity, "请先到「我的钱包」绑定收款方式", Toast.LENGTH_LONG).show()
                }
            }

            // 提现记录 + 余额
            val rl = safeApiRequestCall { api.getWithdrawList(auth) }
            val list = rl?.body()?.data?.list ?: emptyList()
            val bal = rl?.body()?.data?.balance ?: 0L
            val tot = rl?.body()?.data?.total ?: 0L
            val wdSum = rl?.body()?.data?.withdrawn ?: 0L
            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.tv_balance).text = "¥ %.2f".format(bal / 100.0)
                findViewById<TextView>(R.id.tv_total).text = "累计佣金 ¥%.2f".format(tot / 100.0)
                findViewById<TextView>(R.id.tv_done).text = "已提现 ¥%.2f".format(wdSum / 100.0)
                val container = findViewById<LinearLayout>(R.id.container)
                val tvEmpty = findViewById<TextView>(R.id.tv_empty)
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    tvEmpty.visibility = View.GONE
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    for (it in list) {
                        val row = LinearLayout(this@WithdrawActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(0, 20, 0, 20)
                        }
                        val amount = (it.amount ?: 0L) / 100.0
                        val st = when (it.status ?: 0) {
                            1 -> "已打款"
                            2 -> "已拒绝"
                            else -> "审核中"
                        }
                        val t1 = TextView(this@WithdrawActivity).apply {
                            text = df.format(Date((it.created_at ?: 0L) * 1000))
                            setTextColor(0xFF94A3B8.toInt())
                            textSize = 12f
                        }
                        val t2 = TextView(this@WithdrawActivity).apply {
                            text = "¥%.2f  $st".format(amount)
                            setTextColor(if (it.status == 1) 0xFF0A9F52.toInt() else 0xFFD97706.toInt())
                            textSize = 13f
                            setPadding(24, 0, 0, 0)
                        }
                        row.addView(t1)
                        row.addView(t2)
                        container.addView(row)
                    }
                }
            }
        }

        // 提交提现
        findViewById<View>(R.id.btn_submit).setOnClickListener {
            val v = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (v < 100) { Toast.makeText(this, "最低 100 元起提", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (walletStatus != 1) { Toast.makeText(this, "请先到「我的钱包」提交收款信息并通过审核", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            val method = if (rbUsdt.isChecked) "usdt" else "alipay"
            if (method == "usdt" && !usdtBound) { Toast.makeText(this, "USDT 未绑定", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (method == "alipay" && !alipayBound) { Toast.makeText(this, "支付宝未绑定", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            CoroutineScope(Dispatchers.IO).launch {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val r = safeApiRequestCall {
                    api.submitWithdraw(auth, mapOf("amount" to v.toString(), "method" to method))
                }
                val body = r?.body()
                withContext(Dispatchers.Main) {
                    if (body?.status == "success") {
                        Toast.makeText(this@WithdrawActivity, "已提交，等待审核", Toast.LENGTH_LONG).show()
                        etAmount.setText("")
                        recreate()
                    } else {
                        Toast.makeText(this@WithdrawActivity, body?.message ?: "提交失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
