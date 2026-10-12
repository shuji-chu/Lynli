package com.github.kr328.clash

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.safeApiRequestCall
import com.github.kr328.clash.utity.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RedeemActivity : AppCompatActivity() {

    private lateinit var input: EditText

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF4FF"))
        }

        // ---- 顶部栏 ----
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.parseColor("#2E5BFF"))
        }

        val backIcon = ImageView(this).apply {
            setImageResource(com.github.kr328.clash.design.R.drawable.ic_baseline_arrow_back)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(12) }
            setOnClickListener { finish() }
        }
        topBar.addView(backIcon)

        val title = TextView(this).apply {
            text = "兑换码"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        topBar.addView(title)
        root.addView(topBar)

        // ---- 卡片 1：输入 ----
        val card1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.WHITE)
        }
        val bg1 = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.WHITE)
        }
        card1.background = bg1

        val tip = TextView(this).apply {
            text = "请输入兑换码"
            textSize = 15f
            setTextColor(Color.parseColor("#1F2937"))
        }
        card1.addView(tip)

        input = EditText(this).apply {
            hint = "例如：ABCD-1234-EFGH"
            setHintTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            setTextColor(Color.parseColor("#1F2937"))
            setBackgroundColor(Color.parseColor("#F0F4FA"))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        card1.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        val btn = TextView(this).apply {
            text = "立即兑换"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#2E5BFF"))
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { doRedeem() }
        }
        card1.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        root.addView(card1, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(16); marginEnd = dp(16); topMargin = dp(16)
        })

        // ---- 卡片 2：兑换记录（静态占位）----
        val card2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.WHITE)
            }
        }

        val recordTitle = TextView(this).apply {
            text = "兑换记录"
            textSize = 15f
            setTextColor(Color.parseColor("#1F2937"))
        }
        card2.addView(recordTitle)

        val empty = TextView(this).apply {
            text = "暂无兑换记录"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
        }
        card2.addView(empty, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(24); bottomMargin = dp(24) })

        root.addView(card2, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(16); marginEnd = dp(16); topMargin = dp(16)
        })

        setContentView(root)
    }

    private fun doRedeem() {
        val code = input.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) {
            Toast.makeText(this, "请输入兑换码", Toast.LENGTH_SHORT).show()
            return
        }
        LoadingDialog.show(this, "兑换中...")
        CoroutineScope(Dispatchers.IO).launch {
            val apiService = ApiClient.retrofit.create(ApiService::class.java)
            // 后端接口就绪后替换成：
            // val resp = safeApiRequestCall { apiService.redeem(...) }
            runOnUiThread {
                LoadingDialog.hide()
                Toast.makeText(this@RedeemActivity, "兑换接口对接中：$code", Toast.LENGTH_LONG).show()
            }
        }
    }
}
