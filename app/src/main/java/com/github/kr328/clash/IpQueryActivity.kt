package com.github.kr328.clash

import android.os.Bundle
import android.view.View
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

class IpQueryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_ip_query)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_refresh).setOnClickListener { load() }

        load()
    }

    private fun load() {
        val tvIp = findViewById<TextView>(R.id.tv_ip)
        val tvCountry = findViewById<TextView>(R.id.tv_country)
        val tvRegion = findViewById<TextView>(R.id.tv_region)
        val tvCity = findViewById<TextView>(R.id.tv_city)
        val tvIsp = findViewById<TextView>(R.id.tv_isp)
        val tvAs = findViewById<TextView>(R.id.tv_as)

        tvIp.text = "查询中..."

        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            val auth = PreferenceManager.loginauthData ?: ""
            val r = safeApiRequestCall { api.getIP(auth) }
            val d = r?.body()?.data
            withContext(Dispatchers.Main) {
                if (d == null) {
                    tvIp.text = "查询失败"
                    Toast.makeText(this@IpQueryActivity, "查询失败，请重试", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                tvIp.text = d.ip ?: "--"
                tvCountry.text = d.country ?: "--"
                tvRegion.text = d.region ?: "--"
                tvCity.text = d.city ?: "--"
                tvIsp.text = d.isp ?: "--"
                tvAs.text = d.`as` ?: "--"

                val red = android.graphics.Color.parseColor("#E53935")
                val green = android.graphics.Color.parseColor("#2E7D32")

                val tvHosting = findViewById<TextView>(R.id.tv_hosting)
                val tvProxy = findViewById<TextView>(R.id.tv_proxy)
                val tvMobile = findViewById<TextView>(R.id.tv_mobile)

                val h = d.hosting == true
                tvHosting.text = if (h) "是" else "否"
                tvHosting.setTextColor(if (h) red else green)

                val px = d.proxy == true
                tvProxy.text = if (px) "是" else "否"
                tvProxy.setTextColor(if (px) red else green)

                val mb = d.mobile == true
                tvMobile.text = if (mb) "是" else "否"
                tvMobile.setTextColor(if (mb) red else green)
            }
        }
    }
}
