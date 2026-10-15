package com.github.kr328.clash

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.network.APIGlobalObject
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.SubscribeResponse
import com.github.kr328.clash.network.safeApiRequestCall
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrafficDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_traffic_detail)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_go_plans).setOnClickListener {
            startActivity(android.content.Intent(this, PlansActivity::class.java))
            finish()
        }

        // 先用缓存快速渲染
        try {
            val cached = PreferenceManager.cached_userSubscritedata
            if (cached.isNotEmpty()) {
                val gson = Gson()
                val resp = gson.fromJson(cached, SubscribeResponse::class.java)
                render(resp?.data)
            }
        } catch (_: Throwable) {}

        // 后台拉最新
        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            val auth = PreferenceManager.loginauthData ?: ""
            val r = safeApiRequestCall { api.getSubscribe(auth) }
            val d = r?.body()?.data
            if (d != null) {
                try {
                    Gson().toJson(r?.body())?.let { PreferenceManager.cached_userSubscritedata = it }
                } catch (_: Throwable) {}
                withContext(Dispatchers.Main) { render(d) }
            }
        }
    }

    private fun render(d: com.github.kr328.clash.network.SubscribeData?) {
        if (d == null) return
        val usedBytes = (d.u ?: 0L) + (d.d ?: 0L)
        val totalBytes = d.transfer_enable ?: 0L
        val usedGb = usedBytes / 1024.0 / 1024.0 / 1024.0
        val totalGb = totalBytes / 1024.0 / 1024.0 / 1024.0
        val remainGb = (totalGb - usedGb).coerceAtLeast(0.0)
        val percent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

        findViewById<TextView>(R.id.tv_used).text = String.format("%.2f GB", usedGb)
        findViewById<TextView>(R.id.tv_remain).text = String.format("%.2f GB", remainGb)
        findViewById<TextView>(R.id.tv_total).text = String.format("%.0f GB", totalGb)
        findViewById<TextView>(R.id.tv_percent).text = "${percent.coerceIn(0, 100)}%"

        val progress = findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        progress.progress = percent.coerceIn(0, 100)

        findViewById<TextView>(R.id.tv_upload).text = String.format("%.2f GB", (d.u ?: 0L) / 1024.0 / 1024.0 / 1024.0)
        findViewById<TextView>(R.id.tv_download).text = String.format("%.2f GB", (d.d ?: 0L) / 1024.0 / 1024.0 / 1024.0)
        findViewById<TextView>(R.id.tv_plan_name).text = d.plan?.name ?: "未订阅"

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.tv_expire).text = if ((d.expired_at ?: 0L) > 0)
            df.format(Date((d.expired_at ?: 0L) * 1000L)) else "长期有效"

        findViewById<TextView>(R.id.tv_reset).text = "每月 ${d.reset_day ?: "1"} 日"
    }
}
