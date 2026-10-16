package com.github.kr328.clash

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.safeApiRequestCall
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class CashierActivity : AppCompatActivity() {

    private var tradeNo: String = ""
    private var method: String = "alipay"
    private var payUrl: String = ""
    private var paid = false
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tradeNo = intent.getStringExtra("trade_no") ?: ""
        method  = intent.getStringExtra("method") ?: "alipay"
        val amount  = intent.getStringExtra("amount") ?: "0.00"
        val qrText  = intent.getStringExtra("qrcode") ?: ""
        val info    = intent.getStringExtra("pay_info") ?: ""
        payUrl      = intent.getStringExtra("payurl") ?: ""

        if (tradeNo.isEmpty()) {
            Toast.makeText(this, "参数缺失", Toast.LENGTH_SHORT).show(); finish(); return
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.setBackgroundColor(Color.parseColor("#EEF4FF"))

        // 顶栏
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#2E5BFF"))
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }
        topBar.addView(ImageView(this).apply {
            setImageResource(com.github.kr328.clash.design.R.drawable.ic_baseline_close)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(10) }
            setOnClickListener { finish() }
        })
        topBar.addView(TextView(this).apply {
            text = "收银台"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        topBar.addView(LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        topBar.addView(TextView(this).apply {
            text = "我已支付"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.WHITE)
                setColor(Color.TRANSPARENT)
            }
            setOnClickListener { checkNow() }
        })
        root.addView(topBar)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // 金额卡
        val amountCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(24), dp(20), dp(24))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.WHITE)
            }
        }
        amountCard.addView(TextView(this).apply {
            text = "支付金额"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
        })
        amountCard.addView(TextView(this).apply {
            text = "¥ " + amount
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#2E5BFF"))
            setPadding(0, dp(8), 0, dp(8))
        })
        amountCard.addView(TextView(this).apply {
            text = "订单编号：" + tradeNo
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
        })
        content.addView(amountCard)

        // QR 卡
        val qrCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.WHITE)
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(14)
            layoutParams = lp
        }

        val qrString = qrText.ifEmpty { info }
        if (qrString.isNotEmpty()) {
            try {
                val bmp = makeQR(qrString, 560)
                qrCard.addView(ImageView(this).apply {
                    setImageBitmap(bmp)
                    layoutParams = LinearLayout.LayoutParams(dp(240), dp(240))
                })
            } catch (_: Throwable) {
                qrCard.addView(TextView(this).apply {
                    text = "二维码生成失败"
                    textSize = 13f
                    setTextColor(Color.parseColor("#94A3B8"))
                })
            }
        }

        val tip = if (method == "usdt") "使用 TRC20 网络向该地址转账 USDT" else "使用支付宝扫码支付"
        qrCard.addView(TextView(this).apply {
            text = tip
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
            setPadding(0, dp(14), 0, 0)
        })

        if (method == "usdt" && info.isNotEmpty()) {
            qrCard.addView(TextView(this).apply {
                text = info
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(dp(4), dp(8), dp(4), 0)
                setOnClickListener {
                    val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("addr", info))
                    Toast.makeText(this@CashierActivity, "地址已复制", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 打开支付 App 按钮
        if (method != "usdt" && payUrl.isNotEmpty() && (payUrl.startsWith("http") || payUrl.startsWith("alipay"))) {
            qrCard.addView(TextView(this).apply {
                text = "打开支付宝支付"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(40), dp(13), dp(40), dp(13))
                background = GradientDrawable().apply {
                    cornerRadius = dp(26).toFloat()
                    setColor(Color.parseColor("#2E5BFF"))
                }
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(16)
                layoutParams = lp
                setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payUrl)))
                    } catch (_: Throwable) {
                        Toast.makeText(this@CashierActivity, "无法打开链接", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        content.addView(qrCard)

        // 底部
        content.addView(TextView(this).apply {
            text = "由 易搜加速 提供支付服务"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        })

        setContentView(root)

        // 轮询（绑生命周期，页面销毁自动停）
        lifecycleScope.launch {
            repeat(300) {
                if (paid) return@launch
                delay(3000)
                checkPaidSilent()
            }
        }
    }

    private fun checkNow() {
        CoroutineScope(Dispatchers.IO).launch {
            val ok = checkPaidSilent()
            if (!ok) withContext(Dispatchers.Main) {
                Toast.makeText(this@CashierActivity, "还没收到支付，请稍后再试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun checkPaidSilent(): Boolean {
        if (paid) return true
        val api = ApiClient.retrofit.create(ApiService::class.java)
        return try {
            val r = safeApiRequestCall { api.queryOrderPay(PreferenceManager.loginauthData, tradeNo) }
            if (r?.body()?.data?.paid == true) {
                paid = true
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CashierActivity, "支付成功，套餐已开通", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
                true
            } else false
        } catch (_: Throwable) { false }
    }

    private fun makeQR(text: String, size: Int): Bitmap {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.MARGIN] = 1
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.parseColor("#0F1A3C") else Color.WHITE)
        }
        return bmp
    }
}
