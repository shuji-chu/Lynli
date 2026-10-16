package com.github.kr328.clash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.network.APIGlobalObject
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.safeApiRequestCall
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PurchaseSuccessActivity : AppCompatActivity() {

    private lateinit var tvPlan: TextView
    private lateinit var tvExpire: TextView
    private lateinit var tvTraffic: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)

        val dp = resources.displayMetrics.density
        fun dip(v: Int) = (v * dp).toInt()

        // 主容器
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#EEF4FF"))
            setPadding(dip(24), dip(60), dip(24), dip(24))
        }

        // 勾选圆
        val checkView = TextView(this).apply {
            text = "✓"
            textSize = 60f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#22C55E"))
            }
        }
        root.addView(checkView, LinearLayout.LayoutParams(dip(96), dip(96)))

        // 大标题
        root.addView(TextView(this).apply {
            text = "支付成功"
            textSize = 26f
            setTextColor(Color.parseColor("#0F1A3C"))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dip(20)
        })

        // 副标题
        root.addView(TextView(this).apply {
            text = "套餐已激活，可直接使用"
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dip(8)
        })

        // 信息卡片
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dip(14).toFloat()
                setColor(Color.WHITE)
            }
            setPadding(dip(20), dip(20), dip(20), dip(20))
        }

        fun addRow(label: String, tv: TextView) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dip(10), 0, dip(10))
            }
            row.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTextColor(Color.parseColor("#64748B"))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            tv.textSize = 15f
            tv.setTextColor(Color.parseColor("#0F1A3C"))
            tv.gravity = Gravity.END
            row.addView(tv, LinearLayout.LayoutParams(0, -2, 2f))
            card.addView(row)
        }

        tvPlan = TextView(this)
        tvExpire = TextView(this)
        tvTraffic = TextView(this)
        addRow("套餐", tvPlan)
        addRow("到期时间", tvExpire)
        addRow("流量", tvTraffic)

        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dip(28)
        })

        // 立即使用按钮
        val btnUse = TextView(this).apply {
            text = "立即使用"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dip(12).toFloat()
                setColor(Color.parseColor("#2E5BFF"))
            }
            isClickable = true
            setOnClickListener {
                startActivity(Intent(this@PurchaseSuccessActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
                finish()
            }
        }
        root.addView(btnUse, LinearLayout.LayoutParams(-1, dip(52)).apply {
            topMargin = dip(28)
        })

        // 查看订单按钮
        val btnOrders = TextView(this).apply {
            text = "查看订单"
            textSize = 15f
            setTextColor(Color.parseColor("#2E5BFF"))
            gravity = Gravity.CENTER
            setPadding(0, dip(14), 0, dip(14))
            isClickable = true
            setOnClickListener {
                startActivity(Intent(this@PurchaseSuccessActivity, MyOrdersActivity::class.java))
                finish()
            }
        }
        root.addView(btnOrders, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dip(8)
        })

        setContentView(root)

        // 拉最新订阅数据填充
        loadData()
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val r = safeApiRequestCall { api.getSubscribe(auth) }
                val d = r?.body()?.data
                if (d != null) {
                    APIGlobalObject.subData = d
                    try {
                        PreferenceManager.cached_userSubscritedata = Gson().toJson(r.body())
                    } catch (_: Throwable) {}
                    withContext(Dispatchers.Main) {
                        tvPlan.text = d.plan?.name ?: "—"
                        tvExpire.text = if ((d.expired_at ?: 0L) > 0L) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(Date(d.expired_at!! * 1000L))
                        } else "长期有效"
                        val total = (d.transfer_enable ?: 0L) / 1024.0 / 1024.0 / 1024.0
                        tvTraffic.text = String.format(Locale.getDefault(), "%.0f GB", total)
                    }
                }
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PurchaseSuccessActivity, "数据加载失败，回首页可手动刷新", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
