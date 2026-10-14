package com.github.kr328.clash

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.safeApiRequestCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyCustomersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_my_customers)

        val container = findViewById<LinearLayout>(R.id.customer_container)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        CoroutineScope(Dispatchers.IO).launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            val auth = PreferenceManager.loginauthData ?: ""
            val r = safeApiRequestCall { api.getinviteCustomers(auth) }
            val list = r?.body()?.data?.list ?: emptyList()

            withContext(Dispatchers.Main) {
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    return@withContext
                }
                tvEmpty.visibility = View.GONE
                val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                for (c in list) {
                    val item = layoutInflater.inflate(R.layout.item_my_customer, container, false)
                    item.findViewById<TextView>(R.id.tv_name).text = c.nickname ?: "用户"
                    item.findViewById<TextView>(R.id.tv_id).text = "ID: " + (c.short_id ?: 0).toString()
                    item.findViewById<TextView>(R.id.tv_date).text = df.format(Date((c.created_at ?: 0L) * 1000L))
                    item.findViewById<TextView>(R.id.tv_charge).text = "已充值 ¥" + ((c.charge ?: 0L) / 100).toString()
                    item.findViewById<TextView>(R.id.tv_commission).text = "佣金 ¥" + ((c.commission ?: 0L) / 100).toString()
                    container.addView(item)
                }
            }
        }
    }
}
