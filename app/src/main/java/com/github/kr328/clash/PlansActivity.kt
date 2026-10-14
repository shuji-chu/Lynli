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


    private fun _log(msg: String) {
        try {
            val d = java.io.File(getExternalFilesDir(null), "crash"); d.mkdirs()
            java.io.File(d, "plan.txt").appendText("${java.util.Date()} $msg\n")
        } catch (_: Exception) {}
    }

    override fun onCreate() {
        _log("进入 onCreate")
        try {
        referTitle("套餐订阅")

        // 隐藏旧蓝色顶栏，显示新白底顶栏
        try {
            mainBinding?.root?.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.activity_bar_layout)?.visibility = android.view.View.GONE
            val topBar = mainBinding?.root?.findViewById<android.widget.LinearLayout>(com.github.kr328.clash.design.R.id.plan_top_bar)
            topBar?.visibility = android.view.View.VISIBLE
            topBar?.findViewById<android.widget.LinearLayout>(com.github.kr328.clash.design.R.id.plan_btn_support)?.setOnClickListener {
                startActivity(android.content.Intent(this, ProfileActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                })
            }
            topBar?.findViewById<android.widget.FrameLayout>(com.github.kr328.clash.design.R.id.plan_btn_redeem)?.setOnClickListener {
                startActivity(android.content.Intent(this@PlansActivity, RedeemActivity::class.java))
            }
        } catch (_: Throwable) {}

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
        _log("onCreate 完成")
        } catch (e: Throwable) {
            _log("CRASH: " + e.stackTraceToString())
            android.widget.Toast.makeText(this, "套餐页加载出错", android.widget.Toast.LENGTH_LONG).show()
        }
    }


    override suspend fun loadData(page: Int, callback: (List<PlanData>?, Throwable?) -> Unit) {
        try {
            _log("loadData 进入")
            val gson = com.google.gson.Gson()

            var showedCache = false
            if (page == 0) {
                val cached = PreferenceManager.cached_plans
                if (cached.isNotEmpty()) {
                    try {
                        val t = object : com.google.gson.reflect.TypeToken<List<PlanData>>() {}.type
                        val cachedList = gson.fromJson<List<PlanData>>(cached, t)
                        if (!cachedList.isNullOrEmpty()) {
                            callback(cachedList, null)
                            showedCache = true
                            _log("使用缓存 size=" + cachedList.size)
                        }
                    } catch (e: Throwable) {
                        _log("缓存解析失败，忽略: " + e.message)
                    }
                }
            }

            val apiService = ApiClient.retrofit.create(ApiService::class.java)
            val _auth = PreferenceManager.loginauthData ?: ""
            safeApiRequestCall { apiService.getplans(_auth)}.let { resp ->
                _log("resp code=" + (resp?.code() ?: -1) + " ok=" + (resp?.isSuccessful ?: false))
                if (resp != null && resp.isSuccessful) {
                    val data = resp.body()?.data
                    _log("数据 size=" + (data?.size ?: -1))
                    if (page == 0 && !data.isNullOrEmpty()) {
                        try {
                            PreferenceManager.cached_plans = gson.toJson(data)
                        } catch (_: Throwable) {}
                    }
                    callback(data, null)
                    _log("callback OK")
                } else {
                    if (!showedCache) {
                        callback(null, Throwable("请求失败"))
                        _log("callback 失败（无缓存兜底）")
                    } else {
                        _log("请求失败，但已有缓存展示，静默")
                    }
                }
            }
        } catch (e: Throwable) {
            _log("loadData CRASH: " + e.stackTraceToString())
        }
    }

}
