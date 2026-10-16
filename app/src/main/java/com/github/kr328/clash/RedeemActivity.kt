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
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.safeApiRequestCall
import com.github.kr328.clash.utity.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RedeemActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var btn: TextView
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#EAF0FF"), Color.parseColor("#F5F9FF"), Color.parseColor("#EEF4FF"))
        )

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 顶栏
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val back = ImageView(this).apply {
            setImageResource(com.github.kr328.clash.design.R.drawable.ic_baseline_arrow_back)
            setColorFilter(Color.parseColor("#0F1A3C"))
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(8) }
            setOnClickListener { finish() }
        }
        topBar.addView(back)
        topBar.addView(TextView(this).apply {
            text = "兑换码"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
        })
        content.addView(topBar)

        content.addView(TextView(this).apply {
            text = "输入兑换码，立即激活会员或流量"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(dp(4), dp(4), 0, dp(16))
        })

        // === 主卡片（深蓝渐变）===
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1E3A8A"), Color.parseColor("#2E5BFF"))
            ).apply { cornerRadius = dp(20).toFloat() }
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 皇冠
        mainCard.addView(TextView(this).apply {
            text = "👑"
            textSize = 40f
            gravity = Gravity.CENTER
        })

        mainCard.addView(TextView(this).apply {
            text = "易搜加速"
            textSize = 12f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })

        mainCard.addView(TextView(this).apply {
            text = "会员激活中心"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(24))
        })

        // 输入框
        val inputBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.WHITE)
            }
        }
        inputBox.addView(TextView(this).apply {
            text = "🎫"
            textSize = 18f
        })
        input = EditText(this).apply {
            hint = "请输入兑换码"
            setHintTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            setTextColor(Color.parseColor("#0F1A3C"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(10), dp(14), dp(10), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputBox.addView(input)
        mainCard.addView(inputBox, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 按钮
        btn = TextView(this).apply {
            text = "立即兑换"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#2E5BFF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
            background = GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(Color.WHITE)
            }
            setOnClickListener { doRedeem() }
        }
        mainCard.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        content.addView(mainCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) })

        // 特性
        content.addView(TextView(this).apply {
            text = "特致展示"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
            setPadding(dp(4), dp(24), 0, dp(12))
        })

        val feats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        feats.addView(featCard("🖥", "高速流量"))
        feats.addView(space(10))
        feats.addView(featCard("🛡", "专属节点"))
        feats.addView(space(10))
        feats.addView(featCard("⚡", "稳定连接"))
        content.addView(feats)

        // 规则
        val ruleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#FFFFFF"))
                setStroke(dp(1), Color.parseColor("#E2E8F0"))
            }
        }
        ruleCard.addView(TextView(this).apply {
            text = "兑换规则"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
            setPadding(0, 0, 0, dp(10))
        })
        ruleCard.addView(ruleItem("兑换码仅限本人账号使用"))
        ruleCard.addView(ruleItem("激活后权益立即到账"))
        ruleCard.addView(ruleItem("请输入正确码值完成兑换"))
        content.addView(ruleCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) })

        setContentView(root)
        if (PreferenceManager.darkMode == "dark") {
            ThemeHelper.apply(window.decorView, true)
        }
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
                setColor(Color.WHITE)
                setStroke(dp(1), Color.parseColor("#E2E8F0"))
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
            setTextColor(Color.parseColor("#0F1A3C"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        return card
    }

    private fun ruleItem(text: String): TextView {
        return TextView(this).apply {
            this.text = "·  $text"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, dp(4), 0, dp(4))
        }
    }

    private fun doRedeem() {
        val code = input.text?.toString()?.trim()?.uppercase().orEmpty()
        if (code.isEmpty()) {
            Toast.makeText(this, "请输入兑换码", Toast.LENGTH_SHORT).show()
            return
        }
        LoadingDialog.show(this, "兑换中...")
        btn.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            safeApiRequestCall {
                api.redeemCode(PreferenceManager.loginauthData, mapOf("code" to code))
            }.let { resp ->
                withContext(Dispatchers.Main) {
                    LoadingDialog.hide()
                    btn.isEnabled = true
                }
                val body = resp?.body()
                if (resp != null && resp.isSuccessful && body?.code == 1) {
                    val gb = body.data?.reward_gb ?: 0.0
                    val days = body.data?.reward_days ?: 0
                    val parts = mutableListOf<String>()
                    if (gb > 0) parts.add("${gb}GB 流量")
                    if (days > 0) parts.add("${days} 天会员")
                    val msg = if (parts.isEmpty()) "兑换成功" else "兑换成功：" + parts.joinToString(" + ")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RedeemActivity, msg, Toast.LENGTH_LONG).show()
                        input.setText("")
                    }
                } else {
                    val msg = body?.message ?: "兑换失败"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RedeemActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
          } catch (e: Throwable) {
            withContext(Dispatchers.Main) { LoadingDialog.hide(); btn.isEnabled = true }
          }
        }
    }
}
