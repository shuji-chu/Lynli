package com.github.kr328.clash

import android.graphics.Color
import android.graphics.Rect
import android.provider.Settings.Global
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.BaseListActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.adapter.TicketDetailAdapter
import com.github.kr328.clash.design.adapter.TicketsDataAdapter
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.PublicResponse
import com.github.kr328.clash.network.TicketMessage
import com.github.kr328.clash.network.safeApiRequestCall
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class TicketDetailActivity  : BaseListActivity<TicketMessage>() {

    override fun createAdapter(): RecyclerView.Adapter<*> {
        return TicketDetailAdapter(){ planData ->
            // 在这里处理 item 点击事件
        }
    }
    override fun onCreate() {
        setTitle("在线客服")
        showTicketButton {
            startActivity(android.content.Intent(this@TicketDetailActivity, GongdanActivity::class.java))
        }
    }

    private fun setTitle(t: String) {
        mainBinding?.activityBarLayout?.let { bar ->
            bar.findViewById<android.widget.TextView>(
                com.github.kr328.clash.design.R.id.activity_bar_title_view
            )?.text = t
        }
    }

    private fun showTicketButton(onClick: () -> Unit) {
        val btn = android.widget.ImageView(this).apply {
            setImageResource(com.github.kr328.clash.design.R.drawable.ic_ticket_24)
            setPadding(20, 20, 20, 20)
            setOnClickListener { onClick() }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainBinding?.activityBarLayout?.addView(btn)
    }

    fun encodeString(input: String): String {
        val encoded = StringBuilder()

        input.forEach { char ->
            when {
                char.isWhitespace() -> {
                    // 编码空格为 %20
                    encoded.append("")
                }
                char.isLetterOrDigit() || char.toInt() in 0x4E00..0x9FFF -> {
                    // 保留字母、数字和中文字符
                    encoded.append(char)
                }
                else -> {
                    // 其他字符使用 URLEncoder 编码
                    encoded.append(URLEncoder.encode(char.toString(), "UTF-8"))
                }
            }
        }

        return encoded.toString()
    }


    fun onCreateInputView() {

        fun dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 底部输入容器
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            setBackgroundColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }

        // 相机图标
        val cameraIcon = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_camera_24)
            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                Toast.makeText(this@TicketDetailActivity, "相机功能待接", Toast.LENGTH_SHORT).show()
            }
        }
        inputContainer.addView(cameraIcon)

        // 图片图标
        val imageIcon = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_image_24)
            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                Toast.makeText(this@TicketDetailActivity, "图片选择待接", Toast.LENGTH_SHORT).show()
            }
        }
        inputContainer.addView(imageIcon)

        // 输入框
        val editText = EditText(this).apply {
            hint = "请输入消息..."
            setHintTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            setTextColor(Color.parseColor("#1F2937"))
            inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
            minLines = 1
            setBackgroundColor(Color.parseColor("#F0F4FA"))
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(8)
            }
        }
        inputContainer.addView(editText)

        // 发送按钮（蓝色圆形 + 白色箭头）
        val sendButton = android.widget.FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_avatar_user)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
        }
        val sendIcon = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_send_24)
            layoutParams = android.widget.FrameLayout.LayoutParams(dpToPx(20), dpToPx(20), Gravity.CENTER)
        }
        sendButton.addView(sendIcon)

        sendButton.setOnClickListener {
            val message = editText.text.toString()
            if (message.isNullOrEmpty()) {
                Toast.makeText(this@TicketDetailActivity, "请输入内容", Toast.LENGTH_SHORT).show()
            } else {
                val fieldMap = mutableMapOf<String, String>()
                val messageProperty1 = encodeString(message)
                val ticketid = intent.getIntExtra("ticketid", 0)
                fieldMap.put("message", "" + messageProperty1)
                fieldMap.put("id", "" + ticketid)

                GlobalScope.launch {
                    val apiService = ApiClient.retrofit.create(ApiService::class.java)
                    safeApiRequestCall { apiService.replyTicket(PreferenceManager.loginauthData, fieldMap) }.let {
                        if (it != null && it.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TicketDetailActivity, "发送成功", Toast.LENGTH_SHORT).show()
                                editText.setText("")
                            }
                            refereshData()
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TicketDetailActivity, "发送失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        inputContainer.addView(sendButton)

        rootLayout.addView(inputContainer)
        mainBinding?.mainview?.addView(rootLayout)

        // 键盘弹出时把输入栏顶上去
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootLayout.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootLayout.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val params = inputContainer.layoutParams as FrameLayout.LayoutParams
            if (keypadHeight > screenHeight * 0.15) {
                params.bottomMargin = keypadHeight
            } else {
                params.bottomMargin = 0
            }
            inputContainer.layoutParams = params
        }
    }

    override suspend fun loadData(page: Int, callback: (List<TicketMessage>?, Throwable?) -> Unit) {
        val ticketid = intent.getIntExtra("ticketid",0)
        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        safeApiRequestCall { apiService.getTicketsByTID(PreferenceManager.loginauthData,ticketid)}.let {
            if (it != null && it.isSuccessful) {
                withContext(Dispatchers.Main) {

                    referTitle(it.body()?.data?.subject ?: "")
                }
                if(it.body()?.data?.status == 0){
                    withContext(Dispatchers.Main) {
                        onCreateInputView()
                    }
                }
                callback(it.body()?.data?.message,null)

            }else{
                callback(null, Throwable("请求失败"))
            }
        }
    }

}
