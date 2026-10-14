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
            }
        }
    }
}
