package com.github.kr328.clash.design

import android.view.View

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.design.MainDesign.Request
import com.github.kr328.clash.design.databinding.ActivityProfileBinding

import com.github.kr328.clash.design.databinding.DesignProfilesBinding
import com.github.kr328.clash.design.network.APIGlobalObject
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.network.ConfigResponse
import com.github.kr328.clash.network.SubscribeData
import com.github.kr328.clash.network.SubscribeResponse
import com.github.kr328.clash.service.model.Profile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.github.kr328.clash.design.util.BitmapUtil

class ProfileDesign (context: Context) : Design<ProfileDesign.Request>(context) {

    enum class Request {
        OpenBuyPlan,
        OpenMyOrders,
        OpenMyBlance,
        OpenMyInvites,
        OpenBusiness,
        OpenFaq,
        OpenAccount,
        OpenCustomView,
        OpenMyGongdan,
        OpenLogout,
        OpenGSettings,
        OpenCustomViewIPtest,
        OpenCustomViewSpeed,
        OpenTabHome,
        OpenTabPlan,
        OpenTabMine,
        OpenRedeem,
        RefreshSub
    }

    private  var  subData: SubscribeData? = null

    private val binding = ActivityProfileBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root


    init {
        binding.self = this

        binding.navHome.setOnClickListener { request(Request.OpenTabHome) }
        binding.navPlan.setOnClickListener { request(Request.OpenTabPlan) }
        binding.navMine.setOnClickListener { request(Request.OpenTabMine) }
        binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.btn_open_vip)?.setOnClickListener {
            request(Request.OpenBuyPlan)
        }
        binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.btn_invite)?.setOnClickListener {
            request(Request.OpenMyInvites)
        }
        binding.btnRedeem.setOnClickListener { request(Request.OpenRedeem) }
        binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.btn_refresh_sub)?.setOnClickListener {
            request(Request.RefreshSub)
        }
        binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.btn_business)?.setOnClickListener {
            request(Request.OpenBusiness)
        }
        binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.btn_faq)?.setOnClickListener {
            request(Request.OpenFaq)
        }
        binding.root.findViewById<android.view.View>(com.github.kr328.clash.design.R.id.btn_user_card)?.setOnClickListener {
            request(Request.OpenAccount)
        }

//        binding.viewAccountinfo.setBackgroundColor(Color.parseColor("#FFFFFF")) // Semi-transparent white color
//        binding.viewAccountinfo2.setBackgroundColor(Color.parseColor("#FFFFFF")) // Semi-transparent white color
//        binding.viewAccountinfo3.setBackgroundColor(Color.parseColor("#FFFFFF")) // Semi-transparent white color
//        binding.viewAccountinfo4.setBackgroundColor(Color.parseColor("#FFFFFF")) // Semi-transparent white color

    }

    fun request(request: Request) {
        requests.trySend(request)
    }
    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())  // 定义格式
        val date = Date(timestamp)  // 将时间戳转为 Date 对象
        return dateFormat.format(date)  // 格式化为字符串
    }

    fun queryVersion(version: String){
        binding.textVersion.text = version
    }

    suspend fun requestdataDisible() {

        if ( APIGlobalObject.subData == null){
            val gson = Gson()
            val cached_userSubscritedata =  gson.fromJson(PreferenceManager.cached_userSubscritedata, SubscribeResponse::class.java)
            APIGlobalObject.subData =  cached_userSubscritedata.data
        }
        // 显示 ID
        binding.accountEmail = "ID: ----"
        try {
            val _api = com.github.kr328.clash.network.ApiClient.retrofit.create(com.github.kr328.clash.network.ApiService::class.java)
            val _auth = PreferenceManager.loginauthData ?: ""
            val _r = com.github.kr328.clash.network.safeApiRequestCall { _api.getSubscribe(_auth) }
            try {
                if (_r != null && _r.isSuccessful && _r.body() != null) {
                    PreferenceManager.cached_userSubscritedata = Gson().toJson(_r.body())
                }
            } catch (_: Throwable) {}
            var _sid = _r?.body()?.data?.short_id ?: 0
            if (_sid == 0) {
                try {
                    val cached = PreferenceManager.cached_userSubscritedata
                    if (cached.isNotEmpty()) {
                        val _c = Gson().fromJson(cached, SubscribeResponse::class.java)
                        _sid = _c?.data?.short_id ?: 0
                    }
                } catch (_: Throwable) {}
            }
            binding.accountEmail = "ID: " + _sid.toString()
        } catch (_: Throwable) {}

        // 先显示缓存（昵称 + 头像），秒开
        try {
            val cachedNick = PreferenceManager.cached_nickname
            if (cachedNick.isNotBlank()) {
                binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.tv_profile_nickname)?.text = cachedNick
            }
            val cachedAv = PreferenceManager.cached_avatar
            if (cachedAv.startsWith("data:image")) {
                try {
                    val b64 = cachedAv.substringAfter(",")
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    val bmp = BitmapUtil.decodeSampled(bytes, 256, 256)
                    val iv = binding.root.findViewById<android.widget.ImageView>(com.github.kr328.clash.design.R.id.iv_profile_avatar)
                    iv?.setImageBitmap(bmp)
                    iv?.imageTintList = null
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}

        // 拉 user/info 更新昵称 + 头像
        try {
            val _api2 = com.github.kr328.clash.network.ApiClient.retrofit.create(com.github.kr328.clash.network.ApiService::class.java)
            val _auth2 = PreferenceManager.loginauthData ?: ""
            val _r2 = com.github.kr328.clash.network.safeApiRequestCall { _api2.getUserData(_auth2) }
            val _info = _r2?.body()?.data
            val _nick = _info?.nickname ?: ""
            if (_nick.isNotBlank()) {
                binding.root.findViewById<android.widget.TextView>(com.github.kr328.clash.design.R.id.tv_profile_nickname)?.text = _nick
                PreferenceManager.cached_nickname = _nick
            }
            val _av = _info?.avatarURL ?: ""
            if (_av.startsWith("data:image")) {
                try {
                    val b64 = _av.substringAfter(",")
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    val bmp = BitmapUtil.decodeSampled(bytes, 256, 256)
                    val iv = binding.root.findViewById<android.widget.ImageView>(com.github.kr328.clash.design.R.id.iv_profile_avatar)
                    iv?.setImageBitmap(bmp)
                    iv?.imageTintList = null
                    PreferenceManager.cached_avatar = _av
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}

        if ((APIGlobalObject.subData?.plan?.id ?:0) > 0){

            binding.accountPlanname =  APIGlobalObject.subData?.plan?.name ?: ""
            if ( APIGlobalObject.subData?.expired_at == null){
                binding.accountPlantime = "该订阅长期有效"
            }else{
                binding.accountPlantime = "到期时间：${ formatTimestamp((APIGlobalObject.subData?.expired_at ?: 0) * 1000L)}"
            }
            val total = (APIGlobalObject.subData?.plan?.transfer_enable ?: 0L).toDouble() / 1024.0 / 1024.0 / 1024.0
            val downmb = (APIGlobalObject.subData?.u ?: 0.00).toDouble().div(1024).div(1024).div(1024)
            val upmb = (APIGlobalObject.subData?.d ?: 0.00).toDouble().div(1024).div(1024).div(1024)
            val usd =  upmb + downmb
            binding.accountPlanuseinfo = "已用 ${String.format("%.2f", usd)}GB / 总计 ${String.format("%.0f", total)}GB"

            if (total>0 && usd>0) {
                binding.accountProgressValue = (usd /total.toDouble()  * 100).toInt()
                if(total > 0 && usd > total){
                    binding.useedprogressBar.progressTintList =  ColorStateList.valueOf(
//                        context.resolveThemedColor(R.attr.colorOnPrimary)
                        Color.parseColor("#ce665e")
                    )
                }else{

                }
            }else{
                binding.accountProgressValue = 0
            }
            binding.accountBlance = "${ APIGlobalObject.subData?.transfer_enable }"
        }else{
            binding.accountPlanname = "未订阅任何套餐"
            binding.accountPlantime = "已过期"
            binding.accountPlanuseinfo = "已用 0GB/总计 0GB"
        }

    }



}