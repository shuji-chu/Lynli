package com.github.kr328.clash

import android.content.Intent
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.BaseListActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.adapter.PlanDataAdapter
import com.github.kr328.clash.design.view.ActivityBarLayout

import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.PlanData
import com.github.kr328.clash.network.safeApiRequestCall

class PlansActivity : BaseListActivity<PlanData>() {

    override fun createAdapter(): RecyclerView.Adapter<*> {
        return PlanDataAdapter(){ planData ->
            // 在这里处理 item 点击事件
            val intent = Intent(this, ConfigOrderActivity::class.java)
            intent.putExtra("planData", planData) // 使用 Serializable 传递对象
            startActivity(intent)
        }
    }

    override fun onCreate() {
        referTitle("套餐订阅")

        // 底部导航
        val nav = mainBinding?.root?.findViewById<android.widget.LinearLayout>(com.github.kr328.clash.design.R.id.base_bottom_nav)
        nav?.visibility = android.view.View.VISIBLE

        nav?.findViewById<android.widget.LinearLayout>(com.github.kr328.clash.design.R.id.base_nav_home)?.setOnClickListener {
            startActivity(android.content.Intent(this, MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
        }
        nav?.findViewById<android.widget.LinearLayout>(com.github.kr328.clash.design.R.id.base_nav_plan)?.setOnClickListener {
            // 已在本页
        }
        nav?.findViewById<android.widget.LinearLayout>(com.github.kr328.clash.design.R.id.base_nav_mine)?.setOnClickListener {
            startActivity(android.content.Intent(this, ProfileActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
        }

        // 右上角兑换按钮
        setRightButtonVisible {
            startActivity(android.content.Intent(this@PlansActivity, RedeemActivity::class.java))
        }
    }


    override suspend fun loadData(page: Int, callback: (List<PlanData>?, Throwable?) -> Unit) {

        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        safeApiRequestCall { apiService.getplans(PreferenceManager.loginauthData)}.let {
            if (it != null && it.isSuccessful) {
                callback(it.body()?.data,null)
            }else{
                callback(null, Throwable("请求失败"))
            }
        }
    }

}
