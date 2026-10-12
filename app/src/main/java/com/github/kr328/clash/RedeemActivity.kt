package com.github.kr328.clash

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF4FF"))
            setPadding(60, 80, 60, 60)
        }

        val title = TextView(this).apply {
            text = "兑换码"
            textSize = 22f
            setTextColor(Color.parseColor("#1F2937"))
        }
        root.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 })

        val subtitle = TextView(this).apply {
            text = "输入兑换码，立即激活会员或流量"
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
        }
        root.addView(subtitle, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 60 })

        input = EditText(this).apply {
            hint = "请输入兑换码"
            setBackgroundColor(Color.WHITE)
            setPadding(40, 40, 40, 40)
            textSize = 16f
            setTextColor(Color.parseColor("#1F2937"))
        }
        root.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 40 })

        val btn = Button(this).apply {
            text = "立即兑换"
            setBackgroundColor(Color.parseColor("#2E5BFF"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setOnClickListener { doRedeem() }
        }
        root.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            140
        ))

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
            // 后端接口 /user/redeem 就绪后：
            // val resp = safeApiRequestCall { apiService.redeem(auth_data = PreferenceManager.loginToken.orEmpty(), body = RedeemRequest(code)) }
            runOnUiThread {
                LoadingDialog.hide()
                Toast.makeText(this@RedeemActivity, "兑换接口对接中：$code", Toast.LENGTH_LONG).show()
            }
        }
    }
}
