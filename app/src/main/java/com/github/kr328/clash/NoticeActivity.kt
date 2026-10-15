package com.github.kr328.clash

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import com.github.kr328.clash.utity.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoticeActivity : AppCompatActivity() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#EAF0FF"), Color.parseColor("#F5F9FF"), Color.parseColor("#EEF4FF")))

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

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
            text = "公告"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
        })
        content.addView(topBar)

        content.addView(TextView(this).apply {
            text = "最新动态与系统通知"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(dp(4), dp(4), 0, dp(16))
        })

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(listContainer)

        setContentView(root)
        if (PreferenceManager.darkMode == "dark") {
            ThemeHelper.apply(window.decorView, true)
        }

        LoadingDialog.show(this, "加载中...")
        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            safeApiRequestCall { api.getNotices(PreferenceManager.loginauthData) }.let { resp ->
                withContext(Dispatchers.Main) { LoadingDialog.hide() }
                val body = resp?.body()
                if (resp != null && resp.isSuccessful && body?.code == 1) {
                    val list = body.data ?: emptyList()
                    withContext(Dispatchers.Main) {
                        if (list.isEmpty()) {
                            listContainer.addView(TextView(this@NoticeActivity).apply {
                                text = "暂无公告"
                                textSize = 14f
                                setTextColor(Color.parseColor("#64748B"))
                                gravity = Gravity.CENTER
                                setPadding(0, dp(40), 0, dp(40))
                            })
                        } else {
                            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            for (n in list) {
                                listContainer.addView(noticeCard(
                                    n.title ?: "无标题",
                                    n.content ?: "",
                                    fmt.format(Date((n.created_at ?: 0L) * 1000L))
                                ))
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NoticeActivity, "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun noticeCard(title: String, content: String, time: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.parseColor("#E2E8F0"))
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
        })
        if (content.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = content
                textSize = 14f
                setTextColor(Color.parseColor("#475569"))
                setPadding(0, dp(8), 0, 0)
            })
        }
        card.addView(TextView(this).apply {
            text = time
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(10), 0, 0)
        })
        return card
    }
}
