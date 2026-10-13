package com.github.kr328.clash

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        // 根容器：浅蓝渐变背景
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#E8F2FF"), Color.parseColor("#F5F9FF"), Color.parseColor("#EEF4FF"))
        )

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // ============ 顶部栏 ============
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val back = ImageView(this).apply {
            setImageResource(com.github.kr328.clash.design.R.drawable.ic_baseline_arrow_back)
            setColorFilter(Color.parseColor("#1A2547"))
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(8) }
            setOnClickListener { finish() }
        }
        topBar.addView(back)

        val title = TextView(this).apply {
            text = "兑换码"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A2547"))
        }
        topBar.addView(title)
        content.addView(topBar)

        // 副标题
        val sub = TextView(this).apply {
            text = "输入兑换码，立即激活会员或流量"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7A99"))
            setPadding(dp(4), dp(4), 0, dp(16))
        }
        content.addView(sub)

        // ============ 主卡片 ============
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#F0F6FF"))
                setStroke(dp(2), Color.parseColor("#4A90D9"))
            }
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 皇冠图标（用 emoji 占位）
        val crown = TextView(this).apply {
            text = "👑"
            textSize = 36f
            gravity = Gravity.CENTER
        }
        mainCard.addView(crown)

        val brandTag = TextView(this).apply {
            text = "易搜加速"
            textSize = 11f
            setTextColor(Color.parseColor("#6B7A99"))
            gravity = Gravity.CENTER
        }
        mainCard.addView(brandTag)

        val cardTitle = TextView(this).apply {
            text = "易搜加速会员激活中心"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A2547"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(20))
        }
        mainCard.addView(cardTitle)

        // 输入框
        val inputBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.parseColor("#D6E2F5"))
            }
        }
        val inputIcon = TextView(this).apply {
            text = "🎫"
            textSize = 18f
        }
        inputBox.addView(inputIcon)

        input = EditText(this).apply {
            hint = "请输入兑换码"
            setHintTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            setTextColor(Color.parseColor("#1A2547"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(10), dp(14), dp(10), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputBox.addView(input)
        mainCard.addView(inputBox, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 立即兑换按钮（渐变蓝）
        val btn = TextView(this).apply {
            text = "立即兑换"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#5AA8F0"), Color.parseColor("#2E5BFF"))
            ).apply { cornerRadius = dp(26).toFloat() }
            setOnClickListener { doRedeem() }
        }
        mainCard.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        content.addView(mainCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) })

        // ============ 特致展示 ============
        val secTitle = TextView(this).apply {
            text = "特致展示"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A2547"))
            setPadding(dp(4), dp(24), 0, dp(12))
        }
        content.addView(secTitle)

        val feats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        feats.addView(featCard("🖥", "高速流量"))
        feats.addView(space(10))
        feats.addView(featCard("🛡", "专属节点"))
        feats.addView(space(10))
        feats.addView(featCard("⚡", "稳定连接"))
        content.addView(feats)

        // ============ 兑换规则 ============
        val ruleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#F0F6FF"))
                setStroke(dp(1), Color.parseColor("#D6E2F5"))
            }
        }
        val ruleTitle = TextView(this).apply {
            text = "兑换规则"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A2547"))
            setPadding(0, 0, 0, dp(10))
        }
        ruleCard.addView(ruleTitle)
        ruleCard.addView(ruleItem("兑换码仅限本人账号使用"))
        ruleCard.addView(ruleItem("激活后权益立即到账"))
        ruleCard.addView(ruleItem("请输入正确码值完成兑换"))

        content.addView(ruleCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) })

        setContentView(root)
    }

    private fun space(w: Int): android.view.View {
        return android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(w), 1)
        }
    }

    private fun featCard(icon: String, label: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#F0F6FF"))
                setStroke(dp(1), Color.parseColor("#D6E2F5"))
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        card.addView(TextView(this).apply {
            text = icon
            textSize = 22f
            gravity = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.parseColor("#1A2547"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        return card
    }

    private fun ruleItem(text: String): TextView {
        return TextView(this).apply {
            this.text = "·  $text"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7A99"))
            setPadding(0, dp(4), 0, dp(4))
        }
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
            runOnUiThread {
                LoadingDialog.hide()
                Toast.makeText(this@RedeemActivity, "兑换接口对接中：$code", Toast.LENGTH_LONG).show()
            }
        }
    }
}
