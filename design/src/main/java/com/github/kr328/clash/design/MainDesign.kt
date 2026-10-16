package com.github.kr328.clash.design

import com.github.kr328.clash.design.util.showCustomDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficDownload
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.core.util.trafficUpload
import com.github.kr328.clash.design.adapter.BannerItem
import com.github.kr328.clash.design.adapter.ImageSliderImagesAdapter
import com.github.kr328.clash.design.component.ProxyMenu
import com.github.kr328.clash.design.component.ProxyViewConfig
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.preference.selectableList
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.model.AccessControlMode
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.utity.LoadingDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class MainDesign(context: Context) : Design<MainDesign.Request>(context) ,PopupMenu.OnMenuItemClickListener {
    enum class Request {
        ToggleStatus,
        OpenProxy,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
        OpenSettingsDIY,
        OpenSettingsKEFU,
        OpenMineTab,
        OpenSupport,
        OpenModeDirect,
        OpenRedeem,
        OpenNotice,
        OpenAccessControl,
        OpenChangeMode,
        OpenModeGlobal,
        OpenModeRule,
        OpenModeMenu,
        OpenModeSheet,
        OpenTabHome,
        OpenTabPlan,
        OpenTabMine
    }
    private var menu:PopupMenu? = null
    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
        }

    }

    suspend fun setSelectNodeName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.selectnodeName = name
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running
            val outer = binding.root.findViewById<View>(com.github.kr328.clash.design.R.id.connection_ring_outer)
            val inner = binding.root.findViewById<View>(com.github.kr328.clash.design.R.id.connection_ring_inner)
            if (running) {
                binding.trafficStats.visibility = View.VISIBLE
                binding.connectionButtonText.setText("点击断开")
                binding.connectionButtonText.setTextColor(ContextCompat.getColor(context, R.color.white))
                outer?.setBackgroundResource(com.github.kr328.clash.design.R.drawable.bg_connect_outer_red)
                inner?.setBackgroundResource(com.github.kr328.clash.design.R.drawable.bg_connect_inner_red)
            } else {
                binding.connectionButtonText.setText("点击连接")
                binding.connectionButtonText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                outer?.setBackgroundResource(com.github.kr328.clash.design.R.drawable.bg_connect_outer)
                inner?.setBackgroundResource(com.github.kr328.clash.design.R.drawable.bg_connect_inner)
            }
        }
    }


    suspend fun setUploadedSeep(value: Long){
        withContext(Dispatchers.Main) {
            binding.uploadedseed = value.trafficUpload()
        }
    }


    suspend fun setDownloadedSeep(value: Long){
        withContext(Dispatchers.Main) {
            binding.downloadseed = value.trafficDownload()
        }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) {
            binding.forwarded = value.trafficTotal()
        }
    }

    suspend fun setNodeLocation(text: String) {
        withContext(Dispatchers.Main) {
            try {
                binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.node_text)?.text = text
            } catch (_: Throwable) {}
        }
    }

    suspend fun setPlanInfo(planName: String, expireText: String) {
        withContext(Dispatchers.Main) {
            try {
                binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.plan_name_text)?.text = planName
                binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.expire_text)?.text = expireText
            } catch (_: Throwable) {}
        }
    }

    suspend fun setUserTraffic(usedBytes: Long, totalBytes: Long, expireText: String? = null) {
        withContext(Dispatchers.Main) {
            val usedGb = usedBytes / 1024.0 / 1024.0 / 1024.0
            val totalGb = totalBytes / 1024.0 / 1024.0 / 1024.0
            val percent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100) else 0
            try {
                binding.root.findViewById<android.widget.TextView>(R.id.traffic_used_text)
                    ?.text = String.format("%.2f GB", usedGb)
                binding.root.findViewById<android.widget.TextView>(R.id.traffic_total_text)
                    ?.text = String.format("/ %.0f GB", totalGb)
                binding.root.findViewById<android.widget.TextView>(R.id.traffic_percent_text)
                    ?.text = "${percent}%"
                binding.root.findViewById<android.widget.TextView>(R.id.traffic_month_text)
                    ?.text = "本月流量 ${percent}%"
                if (!expireText.isNullOrEmpty()) {
                    binding.root.findViewById<android.widget.TextView>(R.id.traffic_expire_text)
                        ?.text = expireText
                }
                val pb = binding.root.findViewById<android.widget.ProgressBar>(R.id.traffic_progress)
                pb?.progress = percent
                val colorHex = when {
                    percent >= 80 -> "#E53935"
                    percent >= 50 -> "#F59E0B"
                    else -> "#22C55E"
                }
                pb?.progressTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(colorHex)
                )
            } catch (e: Throwable) { println("[setUserTraffic] err: " + e.message) }
        }
    }

    suspend fun setMode(mode: TunnelState.Mode) {
        withContext(Dispatchers.Main) {
            binding.mode = when (mode) {
                TunnelState.Mode.Direct -> context.getString(R.string.direct_mode)
                TunnelState.Mode.Global -> context.getString(R.string.global_mode)
                TunnelState.Mode.Rule -> context.getString(R.string.rule_mode)
                else -> context.getString(R.string.rule_mode)
            }

            when (mode) {
//                TunnelState.Mode.Direct -> {
//                    binding.modeSelectionbutton1.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))
//                }
//                TunnelState.Mode.Global -> binding.modeSelectionbutton2.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))
//                TunnelState.Mode.Rule -> binding.modeSelectionbutton3.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))
//                else -> binding.modeSelectionbutton3.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))

                TunnelState.Mode.Direct -> binding.selectnodeName =  context.getString(R.string.direct_mode)
                TunnelState.Mode.Global -> binding.selectnodeName =  context.getString(R.string.global_mode)
                TunnelState.Mode.Rule -> binding.selectnodeName =  context.getString(R.string.rule_mode)
                else -> binding.selectnodeName =  context.getString(R.string.direct_mode)
            }


        }
    }

    suspend fun setHasProviders(has: Boolean) {
        withContext(Dispatchers.Main) {
            binding.hasProviders = has
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(binding.root)
                .show()
        }
    }

    init {
        try {
            binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.traffic_used_text).text = String.format("已用 %.1fGB / %.0fGB", 0.0, 0.0)
            binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.traffic_percent_text).text = "0%"
        } catch (_: Throwable) {}

        binding.connectionButton?.setOnClickListener { request(Request.ToggleStatus) }

        try {
            val _openCheckin = android.view.View.OnClickListener {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val api = com.github.kr328.clash.network.ApiClient.retrofit.create(com.github.kr328.clash.network.ApiService::class.java)
                    val resp = com.github.kr328.clash.network.safeApiRequestCall {
                        api.getBotStatus(com.github.kr328.clash.design.PreferenceManager.loginauthData)
                    }
                    val d = resp?.body()?.data
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (d == null) {
                            android.widget.Toast.makeText(context, "无法获取绑定状态", android.widget.Toast.LENGTH_SHORT).show()
                            return@withContext
                        }
                        val url = d.mivra_url ?: "https://m.mivra.im/MTProxybot"
                        if (d.bound == true) {
                            // 已绑定 → 显示状态 + 打开机器人
                            try {
                                val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(i)
                            } catch (_: Throwable) {
                                android.widget.Toast.makeText(context, "无法打开 Mivra 机器人", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // 未绑定 → 弹窗提示数字ID + 跳转按钮
                            val guide = (d.guide ?: "打开机器人 → 发送「绑定 {short_id}」 → 完成绑定即可签到")
                                .replace("{short_id}", (d.short_id ?: 0).toString())
                            context.showCustomDialog(
                                title = "绑定 Mivra 机器人",
                                message = "你的数字ID：" + (d.short_id ?: 0) + "\n\n" + guide,
                                positiveButtonText = "打开机器人",
                                negativeButtonText = "取消",
                                onPositiveClick = {
                                    try {
                                        val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(i)
                                    } catch (_: Throwable) {}
                                },
                                onNegativeClick = {}
                            )
                        }
                    }
                }
            }
            binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.checkin_card)?.setOnClickListener(_openCheckin)
            binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.checkin_button)?.setOnClickListener(_openCheckin)
        } catch (_: Throwable) {}

        binding.connectionButtonLottie?.setOnClickListener { request(Request.ToggleStatus) }
        binding.trafficStats?.setOnClickListener { try { val _i = android.content.Intent(); _i.setClassName(context, "com.github.kr328.clash.TrafficDetailActivity"); context.startActivity(_i) } catch (_: Throwable) {} }
        binding.btnRedeem?.setOnClickListener { request(Request.OpenRedeem) }
        binding.btnNotice?.setOnClickListener { request(Request.OpenNotice) }

        // 拉公告未读数 → 红点
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = com.github.kr328.clash.network.ApiClient.retrofit.create(com.github.kr328.clash.network.ApiService::class.java)
                val resp = com.github.kr328.clash.network.safeApiRequestCall {
                    api.getNotices(com.github.kr328.clash.design.PreferenceManager.loginauthData)
                }
                val list = resp?.body()?.data ?: emptyList()
                val unread = list.count { it.read != true }
                withContext(Dispatchers.Main) {
                    binding.root.findViewById<android.view.View>(R.id.notice_dot)?.visibility =
                        if (unread > 0) android.view.View.VISIBLE else android.view.View.GONE
                }
            } catch (_: Throwable) {}
        }
        binding.root.findViewById<android.view.View>(R.id.btn_access_control)?.setOnClickListener {
            request(Request.OpenAccessControl)
        }
        binding.btnSupport.setOnClickListener {
            request(Request.OpenSupport)
        }
        binding.navPlan.setOnClickListener {
            request(Request.OpenSettingsKEFU)
        }
        binding.navMine.setOnClickListener {
            request(Request.OpenMineTab)
        }

        binding.self = this

        binding.colorClashStarted = context.resolveThemedColor(R.attr.colorPrimary)
        binding.colorClashStopped = context.resolveThemedColor(R.attr.colorClashStopped)


//        menu = PopupMenu(context, binding.menuView)
//        menu?.menuInflater?.inflate(R.menu.activity_menu_mode, menu?.menu)
//        menu?.setOnMenuItemClickListener(this)



    }


    override fun onMenuItemClick(item: MenuItem): Boolean {
        item.isChecked = !item.isChecked
        when (item.itemId) {
            R.id.global_mode -> {
                requests.trySend(Request.OpenModeGlobal)
            }
            R.id.rule_mode -> {
                requests.trySend(Request.OpenModeRule)
            }
            else -> return false
        }

        return true
    }

    fun request(request: Request) {
        requests.trySend(request)
    }

    suspend fun startAutoScroll(viewPager2: androidx.viewpager2.widget.ViewPager2,nextItem: Int){

        withContext(Dispatchers.Main) {
            viewPager2.setCurrentItem(nextItem, true)
        }
    }



    suspend fun OpenModeSheet(){

        // 创建 BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(context)

        // 加载自定义的底部布局
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_layout, null)
        bottomSheetDialog.setContentView(view)


        view.findViewById<TextView>(R.id.mode_rule).setOnClickListener {
            // 处理点击事件
            bottomSheetDialog.dismiss()
            requests.trySend(Request.OpenModeRule)

        }
        view.findViewById<TextView>(R.id.mode_global).setOnClickListener {
            // 处理点击事件
            bottomSheetDialog.dismiss()
            requests.trySend(Request.OpenModeGlobal)

        }
        // 显示 BottomSheetDialog
        bottomSheetDialog.show()
    }

    suspend fun OpenModeMenu(){

      menu?.show()

    }

    suspend fun startBannsers() {
        withContext(Dispatchers.Main) {
            //sart ad bannsers
            val viewPager2  = binding.adBanner
            // 创建图片资源列表
           /* val imageList = listOf(
                R.drawable.ad_banner,
                R.drawable.ad_banner2,
                R.drawable.ad_banner3
            )*/

            val cfg = PreferenceManager.getConfigFromPreferences(context)
            val base = cfg?.baseURL?.trimEnd('/') ?: "https://lynli.kz.ci"
            val items = cfg?.banners
                ?.mapNotNull { banner ->
                    val m = banner.media?.takeIf { it.isNotBlank() }
                        ?: banner.image_url?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val url = if (m.startsWith("http://") || m.startsWith("https://")) m
                              else base + "/" + m.trimStart('/')
                    BannerItem(url, banner.title, banner.subtitle, banner.link)
                }
                ?: emptyList()

            //binding.menuView.text = PreferenceManager.modeName
            // 设置适配器
            //val adapter = ImageSliderAdapter(imageList)

            if (items.isNotEmpty()) {

                viewPager2.adapter = ImageSliderImagesAdapter(items) { banner ->
                    val link = banner.link
                    if (!link.isNullOrBlank()) {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(link)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context, "无法打开链接", android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // 3 秒自动轮播
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    var __idx = 0
                    while (true) {
                        kotlinx.coroutines.delay(3000)
                        if (items.isEmpty()) break
                        __idx = (__idx + 1) % items.size
                        try { viewPager2.setCurrentItem(__idx, true) } catch (_: Throwable) {}
                    }
                }
            }

            val uncheckedColor = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_checked)), // 未选中状态
                intArrayOf(Color.parseColor("#40383838")) // 未选中颜色
            )

            val checkedColor = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked)), // 选中状态
                intArrayOf(Color.parseColor("#2b9a45")) // 选中颜色
            )


/*
             val screen = preferenceScreen(context) {
                val vpnDependencies: MutableList<Preference> = mutableListOf()
                val srvStore =  ServiceStore(context)
                val selectMode = selectableList(
                    value = srvStore::accessControlMode,
                    values = AccessControlMode.values(),
                    valuesText = arrayOf(
                        R.string.direct_mode,
                        R.string.rule_mode,
                        R.string.global_mode
                    ),
                    title =  R.string.rule_mode,
                    configure = vpnDependencies::add,
                )
            }
            binding.linearLayoutMode.addView(screen.root)
*/
// modeSelection listener removed (XML is now LinearLayout)
        }



    }




}