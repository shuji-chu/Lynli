package com.github.kr328.clash.design

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
import com.google.android.material.tabs.TabLayoutMediator
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
        OpenChangeMode,
        OpenModeGlobal,
        OpenModeRule,
        OpenModeMenu,
        OpenModeSheet
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
            if (running){
                binding.connectionButton.setAnimation("51a05581.json")
                binding.serverSelection.visibility = View.VISIBLE

//                withContext(Dispatchers.Main){
//                    LoadingDialog.hide()
//                }
                binding.trafficStats.visibility = View.VISIBLE
//                binding.modeImg1.visibility = View.VISIBLE
//                binding.modeImg2.visibility = View.VISIBLE
//                binding.menuView.visibility = View.VISIBLE
                binding.connectionButtonText.setText("点击断开")
                binding.connectionButton.loop(false)
                binding.connectionButtonText.setTextColor(ContextCompat.getColor(context, R.color.white))
            }else{
                binding.trafficStats.visibility = View.GONE
                binding.connectionButton.setAnimation("1d2a0fe5.json")
                binding.connectionButton.loop(true)
                binding.serverSelection.visibility = View.GONE
                binding.connectionButtonText.setText("点击连接")
                binding.connectionButtonText.setTextColor(ContextCompat.getColor(context,R.color.black))
//                binding.modeImg1.visibility = View.GONE
//                binding.modeImg2.visibility = View.GONE
//                binding.menuView.visibility = View.GONE
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

    suspend fun setUserTraffic(usedBytes: Long, totalBytes: Long) {
        withContext(Dispatchers.Main) {
            val usedGb = usedBytes / 1024.0 / 1024.0 / 1024.0
            val totalGb = totalBytes / 1024.0 / 1024.0 / 1024.0
            binding.trafficUsed = String.format("%.2f GB", usedGb)
            binding.trafficTotal = String.format("%.2f GB", totalGb)
            try {
                val r = binding.root
                r.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.traffic_used_text)
                    .text = String.format("%.2f GB / %.2f GB", usedGb, totalGb)
                r.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.traffic_percent_text)
                    .text = percent.coerceIn(0, 100).toString() + "%"
                r.findViewById<android.widget.ProgressBar>(com.github.kr328.clash.design.R.id.traffic_progress)
                    .progress = percent.coerceIn(0, 100)
                r.findViewById<android.widget.ImageView>(com.github.kr328.clash.design.R.id.connection_status_icon)
                    .setImageResource(if (binding.clashRunning) com.github.kr328.clash.design.R.drawable.shield_lock_svgrepo_com else com.github.kr328.clash.design.R.drawable.no_security_svgrepo_com)
            } catch (_: Throwable) {}
            val percent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0
            binding.trafficPercent = percent.coerceIn(0, 100)
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
        binding.btnRedeem?.setOnClickListener { request(Request.OpenRedeem) }
        binding.connectionButton?.setOnClickListener { request(Request.ToggleStatus) }
        binding.serverSelection?.setOnClickListener { request(Request.OpenProxy) }
        binding.trafficStats?.setOnClickListener { request(Request.OpenProxy) }
        binding.btnSupport.setOnClickListener {
            request(Request.OpenSupport)
        }
        binding.navHome.setOnClickListener {
            // 已经是主页，不跳
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
            val tabLayout = binding.tabLayout
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
                // 设置 TabLayoutMediator 来同步 ViewPager2 和 TabLayout
                TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
                    // Tab 的自定义逻辑可以放在这里
                }.attach()
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
            binding.modeSelection.addOnButtonCheckedListener { group, checkedId, isChecked ->

//
//                if(button.id == checkedId){
//                    button.backgroundTintList = checkedColor // 选中状态颜色
//                }else{
//                    button.backgroundTintList = uncheckedColor // 未选中状态颜色
//                }

               /*
                for (buttonId in listOf(binding.modeSelectionbutton1,binding.modeSelectionbutton2,binding.modeSelectionbutton3)) {
                    if (buttonId.id != checkedId) {
//                        binding.modeSelection.uncheck(buttonId.id)
                        buttonId.backgroundTintList = uncheckedColor

                    }
                }


                val button = group.findViewById<MaterialButton>(checkedId)
                button.backgroundTintList = checkedColor // 选中状态颜色
*/

//                binding.modeSelectionbutton1.invalidate()
//                binding.modeSelectionbutton2.invalidate()
//                binding.modeSelectionbutton3.invalidate()

                // Reset all buttons to default background tint
//                binding.modeSelectionbutton1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#40383838"))
//                binding.modeSelectionbutton2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#40383838"))
//                binding.modeSelectionbutton3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#40383838"))
//


                when (checkedId) {
                    R.id.modeSelectionbutton1 -> {
                        request(Request.OpenModeDirect)
                        binding.modeSelectionbutton1.backgroundTintList = checkedColor // 选中状态颜色
                       // binding.modeSelectionbutton1.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))
                    }

                    R.id.modeSelectionbutton2 -> {

                        request(Request.OpenModeGlobal)
                        binding.modeSelectionbutton2.backgroundTintList = checkedColor // 选中状态颜色
                        //binding.modeSelectionbutton2.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))
                    }

                    R.id.modeSelectionbutton3 -> {
                        request(Request.OpenModeRule)
                        binding.modeSelectionbutton3.backgroundTintList = checkedColor // 选中状态颜色
                       // binding.modeSelectionbutton3.backgroundTintList =  ColorStateList.valueOf(Color.parseColor("#2b9a45"))
                    }
                }


            }
        }



    }




}