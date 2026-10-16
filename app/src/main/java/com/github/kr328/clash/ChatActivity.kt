package com.github.kr328.clash

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.ChatMessageData
import com.github.kr328.clash.network.safeApiRequestCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class ChatActivity : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private var lastTs: Long = 0
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.setBackgroundColor(Color.parseColor("#EEF4FF"))

        // 顶栏
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(2).toFloat()
        }
        topBar.addView(ImageView(this).apply {
            setImageResource(com.github.kr328.clash.design.R.drawable.ic_baseline_arrow_back)
            setColorFilter(Color.parseColor("#0F1A3C"))
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) }
            setOnClickListener { finish() }
        })
        topBar.addView(TextView(this).apply {
            text = "在线客服"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F1A3C"))
        })
        topBar.addView(LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        topBar.addView(TextView(this).apply {
            text = "AI 秒回"
            textSize = 12f
            setTextColor(Color.parseColor("#0A9F52"))
        })
        root.addView(topBar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 消息区
        scroll = ScrollView(this).apply { isFillViewport = true }
        messageContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        scroll.addView(messageContainer)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // 底部输入
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        input = EditText(this).apply {
            hint = "输入消息..."
            textSize = 14f
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.parseColor("#0F1A3C"))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#F5F7FB"))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 4
        }
        bottom.addView(input)
        val sendBtn = TextView(this).apply {
            text = "发送"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#2E5BFF"))
            }
            setPadding(dp(18), dp(10), dp(18), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
            setOnClickListener { send() }
        }
        bottom.addView(sendBtn)
        root.addView(bottom, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(root)

        loadHistory(initial = true)

        lifecycleScope.launch {
            while (true) {
                delay(2500)
                loadHistory(initial = false)
            }
        }
    }

    private fun loadHistory(initial: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            safeApiRequestCall {
                api.chatHistory(PreferenceManager.loginauthData, if (initial) 0 else lastTs)
            }.let { resp ->
                val body = resp?.body()
                if (resp != null && resp.isSuccessful && body?.code == 1) {
                    val list = body.data?.messages ?: emptyList()
                    if (list.isNotEmpty()) {
                        val maxTs = list.maxOfOrNull { it.created_at ?: 0L } ?: lastTs
                        lastTs = maxOf(lastTs, maxTs)
                        withContext(Dispatchers.Main) {
                            for (m in list) addBubble(m)
                            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                        }
                    }
                }
            }
        }
    }

    private fun addBubble(m: ChatMessageData) {
        val isMe = m.sender == "user"
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMe) Gravity.END else Gravity.START
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val bubble = TextView(this).apply {
            text = m.content ?: ""
            textSize = 14f
            maxWidth = (resources.displayMetrics.widthPixels * 0.72).toInt()
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setTextColor(if (isMe) Color.WHITE else Color.parseColor("#0F1A3C"))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (isMe) Color.parseColor("#2E5BFF") else Color.WHITE)
            }
        }
        row.addView(bubble)
        messageContainer.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun send() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        input.setText("")
        val now = System.currentTimeMillis() / 1000
        addBubble(ChatMessageData(id = null, sender = "user", content = text, created_at = now))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            safeApiRequestCall {
                api.chatSend(PreferenceManager.loginauthData, mapOf("content" to text))
            }.let { resp ->
                if (resp == null || !resp.isSuccessful || resp.body()?.code != 1) {
                    val detail = buildString {
                        append("发送失败")
                        if (resp == null) append(" [null]")
                        else {
                            append(" HTTP=" + resp.code())
                            try { append(" body=" + (resp.errorBody()?.string() ?: resp.body()?.message ?: "")) } catch (_: Throwable) {}
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, detail, Toast.LENGTH_LONG).show()
                    }
                } else {
                    delay(2500)
                    loadHistory(false)
                }
            }
        }
    }
}
