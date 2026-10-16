package com.github.kr328.clash

import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.core.bridge.*
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.NewProfileDesign
import com.github.kr328.clash.design.PreferenceLiveData
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.component.ProxyMenu
import com.github.kr328.clash.design.databinding.ActivitySplashBinding
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.dialog.withModelProgressBar
import com.github.kr328.clash.design.network.APIGlobalObject
import com.github.kr328.clash.design.util.ValidatorHttpUrl
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.design.util.showCustomDialog
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiClientConfig
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.ConfigResponse
import com.github.kr328.clash.network.LoginRequest
import com.github.kr328.clash.network.SubscribeData
import com.github.kr328.clash.network.SubscribeResponse
import com.github.kr328.clash.network.safeApiCall
import com.github.kr328.clash.network.safeApiRequestCall
import com.github.kr328.clash.service.ProfileManager
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.remote.IRemoteService
import com.github.kr328.clash.service.util.PreferenceManagerServer
import com.github.kr328.clash.utity.LoadingDialog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import com.github.kr328.clash.remote.Remote

class MainActivity : BaseActivity<MainDesign>() {

    private var trafficTimer: kotlinx.coroutines.Job? = null
    private var lastForeground = false

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+ 请求通知权限（用户首次打开弹一次）
        requestNotificationPermission()

        // 全局崩溃捕获：只记日志，不再给用户看堆栈
        val __prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, throwable ->
            try {
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                val stack = sw.toString()
                try {
                    java.io.File(filesDir, "crash.txt").writeText(stack)
                } catch (_: Exception) {}

                // 非主线程崩溃：只记日志，不弹窗，不闪退
                if (t !== android.os.Looper.getMainLooper().thread) {
                    return@setDefaultUncaughtExceptionHandler
                }

                // 主线程崩溃：弹一句人话，然后走系统默认流程（进程重启）
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        val tv = android.widget.TextView(applicationContext)
                        tv.text = "抱歉，程序遇到了一点小问题。\n\n请点击下方按钮重新进入。"
                        tv.setPadding(60, 60, 60, 60)
                        tv.textSize = 15f
                        tv.setTextColor(0xFF333333.toInt())
                        val sv = android.widget.ScrollView(applicationContext)
                        sv.addView(tv)
                        android.app.AlertDialog.Builder(applicationContext)
                            .setTitle("出错了")
                            .setView(sv)
                            .setCancelable(false)
                            .setPositiveButton("重新进入") { _, _ ->
                                try {
                                    val i = android.content.Intent(applicationContext, MainActivity::class.java)
                                    i.addFlags(
                                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    )
                                    applicationContext.startActivity(i)
                                } catch (_: Throwable) {}
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                            .show()
                    } catch (_: Throwable) {
                        __prevHandler?.uncaughtException(t, throwable)
                    }
                }
            } catch (_: Throwable) {
                __prevHandler?.uncaughtException(t, throwable)
            }
        }

        UpdateChecker.check(this)
    }



    private lateinit var apiService: ApiService
    private var isSubscribing = false
    private var _lastSubStart = 0L
    private  var  subData: SubscribeData? = null



    private val selectnodeName: LiveData<String> by lazy {
        PreferenceLiveData(this)
    }

    override suspend fun main() {
        val design = MainDesign(this)

        if (PreferenceManager.isLoginin){
            setContentDesign(design)

            // 骨架屏：先显示灰色占位，数据回来后淡出
            try { design?.setLoading(true) } catch (_: Throwable) {}

            // 大厂模式：立即出 UI，网络/IO 全部丢后台静默
            try { com.github.kr328.clash.utity.LoadingDialog.hide() } catch (_: Throwable) {}

            // 骨架屏淡出（独立协程，最多 800ms 强制淡出，不受 fetch 卡住影响）
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(800)
                try { design?.setLoading(false) } catch (_: Throwable) {}
            }

            // 数据加载（独立协程，慢也不挡 UI）
            CoroutineScope(Dispatchers.IO).launch {
                try { design?.fetch() } catch (_: Throwable) {}
                try { design?.startBannsers() } catch (_: Throwable) {}
            }


            //1 : 查询 config 信息

            apiService = ApiClientConfig.retrofit.create(ApiService::class.java)

            //获取 Config 数据
//        CoroutineScope(Dispatchers.IO).launch {   }



            val currentTime = System.currentTimeMillis()
            val cacheExpiryTime = 5 * 60 * 1000

            // 进 APP 时后台静默预下载配置（下次连接秒开）
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val _has = withProfile { queryActive() } != null
                        val _url = subData?.subscribe_url ?: ""
                        val _pname = subData?.plan?.name ?: ""
                        val _pid = subData?.plan?.id ?: 0
                        if (!_has && _url.isNotEmpty() && _pname.isNotEmpty() && _pid > 0) {
                            val _u = withProfile {
                                create(Profile.Type.Url, _pname).also { patch(it, _pname, _url, 0) }
                            }
                            withProfile { try { commit(_u) { } } catch (_: Throwable) {} }
                        }
                    } catch (e: Throwable) { android.util.Log.d("Lynli", "[prefetch] err: " + e.message) }
                }
            } catch (_: Throwable) {}

            // 大厂模式：先用缓存秒渲，再后台拉最新覆盖
            try {
                val _cached = PreferenceManager.cached_userSubscritedata
                if (_cached.isNotEmpty()) {
                    val _resp = Gson().fromJson(_cached, SubscribeResponse::class.java)
                    val _d = _resp?.data
                    if (_d != null) {
                        design?.setUserTraffic(((_d.u ?: 0) + (_d.d ?: 0)).toLong(), (_d.transfer_enable ?: 0).toLong())
                        try {
                            val pName = _d.plan?.name ?: "标准套餐"
                            val exp = _d.expired_at ?: 0L
                            val expTxt = if (exp > 0) {
                                "到期时间：" + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(exp * 1000L))
                            } else "到期时间 --"
                            design?.setPlanInfo(pName, expTxt)
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}

            // 进 APP 无条件拉一次订阅，保证首页流量卡有数据
            try { performSubscrite() } catch (_: Throwable) {}


// 如果缓存数据存在且未超过 10 分钟，使用缓存数据；否则，重新请求数据
            if (  PreferenceManager.cached_data != null && (currentTime - PreferenceManager.cache_timestamp) <= cacheExpiryTime)  {
                // 使用缓存数据
                val gson = Gson()
                val configResponse =  gson.fromJson(PreferenceManager.cached_data, ConfigResponse::class.java)
                if (PreferenceManager.cached_userSubscritedata.length > 0){
                    val subcache =  gson.fromJson(PreferenceManager.cached_userSubscritedata, SubscribeResponse::class.java)

                    // 用缓存更新流量卡（避免 5 分钟内进 APP 显示旧数据）
                    try {
                        val _d = subcache.data
                        if (_d != null) {
                            val _used = (_d.u ?: 0) + (_d.d ?: 0)
                            val _total = _d.transfer_enable ?: 0
                            design?.setUserTraffic(_used.toLong(), _total.toLong())
                            try {
                                val pName = _d.plan?.name ?: "标准套餐"
                                val exp = _d.expired_at ?: 0L
                                val expTxt = if (exp > 0) {
                                    "到期时间：" + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(exp * 1000L))
                                } else "到期时间 --"
                                design?.setPlanInfo(pName, expTxt)
                            } catch (_: Throwable) {}
                        }
                    } catch (e: Throwable) {
                    }
                    subData =  subcache.data

                    //存储到本地
                    val plainid = subcache.data.plan_id ?: 0
//                    或者检查使用量
                    val guoqi = (subcache.data.u ) +  (subcache.data.d) >  (subcache.data.transfer_enable ?: 0) && (subcache.data.transfer_enable ?: 0) > 0
                    if ( plainid == 0 || guoqi ) {
                        //提示到期
                        //删除所有节点
                        withProfile {
                            var allProfiles = queryAll()
                            //删除所有节点
                            if (allProfiles.isNotEmpty() && allProfiles.count() > 0){
                                allProfiles.forEach {
                                    delete(it.uuid)
                                }
                            }

                        }

                        //删除所有节点文件


                    }

                }




            }else{
                // 缓存已过期，后台静默刷新（不弹窗，避免打扰用户）
                configRequesting()

            }




            selectnodeName.observe(this, androidx.lifecycle.Observer { newValue ->
            })

            val ticker = ticker(TimeUnit.SECONDS.toMillis(1))

            while (isActive) {
                select<Unit> {
                    events.onReceive {
                        when (it) {
                            Event.ActivityStart,
                            Event.ServiceRecreated,
                            Event.ClashStop, Event.ClashStart,
                            Event.ProfileLoaded, Event.ProfileChanged -> design.fetch()
                            is Event.ConnectionStateChanged -> {
                                try { design?.setConnectionState(it.state) } catch (_: Throwable) {}
                            }
                            else -> Unit
                        }
                    }
                    design.requests.onReceive {
                        when (it) {
                            MainDesign.Request.OpenTabHome -> { /* 已在主页 */ }
                            MainDesign.Request.OpenTabPlan -> {
                                startActivity(PlansActivity::class.intent)
                            }
                            MainDesign.Request.OpenTabMine -> {
                                startActivity(ProfileActivity::class.intent)
                            }
                            MainDesign.Request.ToggleStatus -> {
                                _log("ToggleStatus clashRunning=" + clashRunning + " state=" + Remote.broadcasts.connectionState)
                                // 防抖：正在连接/断开中，忽略点击
                                if (Remote.broadcasts.connectionState.isBusy) {
                                    _log("ToggleStatus ignored: busy")
                                } else if (clashRunning) {
                                    // 立即切 Disconnecting + 锁定 3 秒，防广播抖动
                                    Remote.broadcasts.setConnectionState(
                                        com.github.kr328.clash.design.state.ConnectionState.Disconnecting,
                                        lockMs = 3000
                                    )
                                    this@MainActivity.launch { try { design?.setConnectionState(com.github.kr328.clash.design.state.ConnectionState.Disconnecting) } catch (_: Throwable) {} }
                                    stopClashService()
                                } else {
                                    // 立即切 Connecting + 锁定 5 秒，防广播抖动
                                    Remote.broadcasts.setConnectionState(
                                        com.github.kr328.clash.design.state.ConnectionState.Connecting,
                                        lockMs = 5000
                                    )
                                    this@MainActivity.launch { try { design?.setConnectionState(com.github.kr328.clash.design.state.ConnectionState.Connecting) } catch (_: Throwable) {} }
                                    design.startClash()
                                }
                            }
                            MainDesign.Request.OpenProxy ->
                                startActivity(ProxyActivity::class.intent)

                            MainDesign.Request.OpenProfiles ->
                                startActivity(ProfilesActivity::class.intent)
                            MainDesign.Request.OpenProviders ->
                                startActivity(ProvidersActivity::class.intent)
                            MainDesign.Request.OpenLogs ->
                                startActivity(LogsActivity::class.intent)
                            MainDesign.Request.OpenSettings ->
                                startActivity(SettingsActivity::class.intent)
                            MainDesign.Request.OpenHelp ->
                                startActivity(HelpActivity::class.intent)
                            MainDesign.Request.OpenAbout ->
                                design.showAbout(queryAppVersionName())
                            MainDesign.Request.OpenSettingsDIY ->
                                startActivity(ProfileActivity::class.intent)
                            MainDesign.Request.OpenSettingsKEFU ->
                                startActivity(PlansActivity::class.intent)
                            MainDesign.Request.OpenMineTab -> {
                                startActivity(android.content.Intent(this@MainActivity, ProfileActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                })
                            }
                            MainDesign.Request.OpenSupport ->
                                startActivity(GongdanActivity::class.intent)
                            MainDesign.Request.OpenRedeem ->
                                startActivity(android.content.Intent(this@MainActivity, RedeemActivity::class.java))
                            MainDesign.Request.OpenNotice ->
                                startActivity(android.content.Intent(this@MainActivity, NoticeActivity::class.java))
                            MainDesign.Request.OpenAccessControl ->
                                startActivity(android.content.Intent(this@MainActivity, AccessControlActivity::class.java))

                            MainDesign.Request.OpenModeDirect ->

                                 withClash {
                                    PreferenceManager.modeName =  TunnelState.Mode.Direct.name
                                    val override = queryOverride(Clash.OverrideSlot.Session)
                                     override.mode = TunnelState.Mode.Direct
                                    patchOverride(Clash.OverrideSlot.Session, override)

                                }

                            MainDesign.Request.OpenModeGlobal ->  withClash {
                                CoroutineScope(Dispatchers.Main).launch {
                                    PreferenceManager.modeName =  TunnelState.Mode.Global.name
                                    LoadingDialog.show(this@MainActivity, "正在切换中...")

                                    val names = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }

                                    withClash {
                                        val o = queryOverride(Clash.OverrideSlot.Session)
                                        o.mode = TunnelState.Mode.Global
                                        patchOverride(Clash.OverrideSlot.Session, o)


                                    }
                                    startServer()

                                }
                            }
                            MainDesign.Request.OpenModeRule ->  withClash {

                                CoroutineScope(Dispatchers.Main).launch {
                                    PreferenceManager.modeName =  TunnelState.Mode.Rule.name
                                    LoadingDialog.show(this@MainActivity, "正在切换中...")
                                        withClash {
                                            val o = queryOverride(Clash.OverrideSlot.Session)
                                            o.mode = TunnelState.Mode.Rule
                                            patchOverride(Clash.OverrideSlot.Session, o)

                                        }
                                    startServer()

                                }
                            }


                            MainDesign.Request.OpenModeMenu -> {
                                design.OpenModeMenu()
                            }

                            MainDesign.Request.OpenChangeMode -> {
                                design.OpenModeMenu()
                            }

                            MainDesign.Request.OpenModeSheet ->{
                                design.OpenModeSheet()
                            }
                        }
                    }
                    if (clashRunning) {
                        ticker.onReceive {
                            design.fetchTraffic()
                        }
                    }
                }
            }



        }else{
            val binding = ActivitySplashBinding
                .inflate(this.layoutInflater, this.root, false)

            setContentView(binding.root)

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(LoginActivity::class.intent)

            }, 1000) // Simulating a network delay
        }

    }

    private suspend fun configRequesting(retryCount: Int = 0){
        if (PreferenceManager.loginToken == "demo-token") {
            withContext(Dispatchers.Main) { LoadingDialog.hide() }
            return
        }
        val gson = Gson()
        val currentTime = System.currentTimeMillis()
        val env = try {
            safeApiCall { apiService.getConfig() }
        } catch (e: Exception) {
            null
        }
        if (env != null && env.code == 1) {
            try {
                PreferenceManager.saveConfigToPreferences(env)
                PreferenceManager.cache_timestamp = currentTime
                PreferenceManager.cached_data = gson.toJson(env)
            } catch (e: Exception) {
            }
        } else {
            var restored = false
            try {
                val cached = PreferenceManager.cached_data
                if (!cached.isNullOrEmpty()) {
                    val cfg = gson.fromJson(cached, ConfigResponse::class.java)
                    if (cfg != null && cfg.baseURL.isNotEmpty()) {
                        PreferenceManager.saveConfigToPreferences(cfg)
                        restored = true
                    }
                }
            } catch (e: Exception) {
            }
            if (!restored) {
                try {
                    PreferenceManager.saveConfigToPreferences(ConfigResponse(baseURL = "https://lynli.kz.ci"))
                } catch (e: Exception) {
                }
            }
        }
        try {
            withContext(Dispatchers.Main) { LoadingDialog.hide() }
        } catch (e: Exception) {
        }
        try {
            performSubscrite()
            loadNodeLocation()
        } catch (e: Exception) {
        }
    }

    private fun loadNodeLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val r = com.github.kr328.clash.network.safeApiRequestCall { api.getIP(auth) }
                val d = r?.body()?.data
                if (d != null) {
                    val c = d.country ?: ""
                    val city = d.city ?: ""
                    val region = d.region ?: ""
                    val loc = when {
                        c.isNotEmpty() && city.isNotEmpty() -> "$c·$city"
                        c.isNotEmpty() && region.isNotEmpty() -> "$c·$region"
                        c.isNotEmpty() -> c
                        else -> d.ip ?: ""
                    }
                    if (loc.isNotEmpty()) {
                        design?.setNodeLocation("当前节点：$loc")
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun performSubscrite(){
        val _nowMs = System.currentTimeMillis()
        if (_nowMs - _lastSubStart < 5000) return
        _lastSubStart = _nowMs

        // 演示模式：跳过网络请求 / Demo mode: skip network
        if (PreferenceManager.loginToken == "demo-token") {
            LoadingDialog.hide()
            return
        }

        val apiServiceApp = ApiClient.retrofit.create(ApiService::class.java)

        //获取 Config 数据
        CoroutineScope(Dispatchers.IO).launch {
            safeApiRequestCall {
                apiServiceApp.getSubscribe(PreferenceManager.loginauthData)}.let {
                withContext(Dispatchers.Main) {
                    LoadingDialog.hide()
                }
                if (it != null &&
                    it.isSuccessful) {
                    subData = it.body()?.data

                    // 首页流量卡片：显示真实已用/总量/百分比
                    it.body()?.data?.let { d ->
                        val used = (d.u ?: 0) + (d.d ?: 0)
                        val total = d.transfer_enable ?: 0
                        design?.setUserTraffic(used, total)
                        try {
                            val pName = d.plan?.name ?: "标准套餐"
                            val exp = d.expired_at ?: 0L
                            val expTxt = if (exp > 0) {
                                "到期时间：" + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(exp * 1000L))
                            } else "到期时间 --"
                            design?.setPlanInfo(pName, expTxt)
                        } catch (_: Throwable) {}
                    }

                    val gson = Gson()
                    PreferenceManager.cached_userSubscritedata =  gson.toJson( it.body())
                    APIGlobalObject.subData =  it.body()?.data
                    //存储到本地
                    val plainid = it.body()?.data?.plan_id ?: 0
//                    或者检查使用量
                    val guoqi = (it.body()?.data?.u ?: 0) +  (it.body()?.data?.d ?: 0) >  (it.body()?.data?.transfer_enable ?: 0) && (it.body()?.data?.transfer_enable ?: 0) > 0
                    if ( plainid == 0 || guoqi ) {
                        //提示到期
                        //删除所有节点
                        withProfile {
                            var allProfiles = queryAll()
                            if (allProfiles.isNotEmpty() && allProfiles.count() > 1){
                                allProfiles.forEach { delete(it.uuid) }
                               //删除所有节点
                            }
                        }

                        //删除所有节点文件


                    }else{
                        //更新本地节点
                        withProfile {
                            var profileActive = queryActive()
                            if (profileActive != null){
                                try {
                                    update(profileActive.uuid)
                                }catch (e: Exception){
                                }
                                finally {
                                }
                            }

                        }
                    }
                }
            }
        }
    }


    private suspend fun MainDesign.fetch() {
        withContext(Dispatchers.IO) {
            Remote.broadcasts.refreshRunningState()
        }
        setClashRunning(Remote.broadcasts.clashRunning)

        val state = withClash {
            queryTunnelState()
        }


        val providers = withClash {
            queryProviders()
        }

        if (providers.isNotEmpty()) {
            providers.forEach { android.util.Log.d("Lynli", "The element is ${it.name}") }
        }

        setMode(state.mode)
        setHasProviders(providers.isNotEmpty())

        withProfile {
            setProfileName(queryActive()?.name)
            setSelectNodeName(PreferenceManager.selectnodeName )
        }
    }

    //查询上传速度和下载速度
    private suspend fun MainDesign.fetchTraffic() {
        withClash {

//            setForwarded(queryTrafficTotal())

            setUploadedSeep(queryTrafficTotal())
            setDownloadedSeep(queryTrafficTotal())
        }
    }

    private suspend fun MainDesign.startClash() {

        // 傻瓜模式核心：每次点连接前，先拉一次最新订阅
        // 用户买了新套餐 / 后端换了节点 → 订阅 URL 变化 → 自动重建 profile
        // 用户完全无感，不用手动刷
        _log("startClash 准备拉最新订阅")
        try {
            withContext(Dispatchers.IO) {
                val apiServiceApp = ApiClient.retrofit.create(ApiService::class.java)
                safeApiRequestCall {
                    apiServiceApp.getSubscribe(PreferenceManager.loginauthData)
                }?.let { resp ->
                    if (resp.isSuccessful) {
                        val newData = resp.body()?.data
                        if (newData != null) {
                            subData = newData
                            APIGlobalObject.subData = newData
                            try {
                                PreferenceManager.cached_userSubscritedata =
                                    Gson().toJson(resp.body())
                            } catch (_: Throwable) {}
                            _log("startClash 订阅已刷新 plan=" + (newData.plan?.name ?: "?")
                                 + " sub_url=" + (newData.subscribe_url ?: "").take(60))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _log("startClash 刷订阅失败（不阻塞连接）: " + e.message)
        }

        // 不再每次删除配置，让第二次连接秒开


//        val activeProfile = withProfile { queryActive() }
//        if (activeProfile != null) {
//            android.util.Log.i("activeProfile",">> ${activeProfile.type.name}  ${activeProfile.uuid} ${activeProfile.source} ")
//            showToast(R.string.no_profile_selected, ToastDuration.Long) {
//                setAction(R.string.profiles) {
//                    startActivity(ProfilesActivity::class.intent)
//                }
//            }

//        }
        val activeProfile = withProfile { queryActive() }
        _log("startClash activeProfile=" + activeProfile)

        // 傻瓜模式核心：对比现有 profile 和最新 subData.subscribe_url
        // 不一致（用户买了新套餐 / 后端换节点）→ 删旧建新，用户无感
        val latestSubUrl = subData?.subscribe_url ?: ""
        val needRebuild = activeProfile != null &&
                          latestSubUrl.isNotEmpty() &&
                          !activeProfile.source.contains(
                              java.net.URLEncoder.encode(latestSubUrl, "utf-8"),
                              ignoreCase = true
                          ) &&
                          !latestSubUrl.contains(activeProfile.source, ignoreCase = true)

        if (needRebuild) {
            _log("startClash 订阅 URL 变化，删旧建新")
            _log("  old=" + (activeProfile?.source ?: "").take(80))
            _log("  new=" + latestSubUrl.take(80))
            try {
                withProfile { delete(activeProfile!!.uuid) }
                _log("startClash 旧 profile 已删")
            } catch (e: Exception) {
                _log("startClash 删旧 profile 失败: " + e.message)
            }
        }

        // 重新查询（因为可能刚删了）
        val activeProfile2 = withProfile { queryActive() }
        if (activeProfile2 == null) {
            _log("startClash 无 profile，开始创建")
            LoadingDialog.show(this@MainActivity, "正在请求节点数据...")
            launch {
                try {
                    val suburl = subData?.subscribe_url ?: ""
                    val planName = subData?.plan?.name ?: "VPN"
                    if (suburl.isEmpty()) {
                        _log("startClash 无 sub_url，触发 performSubscrite")
                        performSubscrite()
                        return@launch
                    }
                    var newULR = suburl
                    if (ApiClientConfig.ConfigNodeURL.length > 3) {
                        val encodeURL = java.net.URLEncoder.encode(suburl, "utf-8").replace("?", "%3F")
                        newULR = ApiClientConfig.ConfigNodeURL + encodeURL
                    }
                    _log("startClash 创建 profile name=" + planName)
                    val uuid2 = withProfile {
                        create(Profile.Type.Url, planName).also {
                            patch(it, planName, newULR, 0)
                        }
                    }
                    _log("startClash profile created uuid=" + uuid2)
                    withProfile { commit(uuid2) }
                    _log("startClash commit done")
                    activeProfile()
                    _log("startClash profile activated")
                    withContext(Dispatchers.Main) { LoadingDialog.hide() }
                    startServer()
                } catch (e: Exception) {
                    _log("startClash 创建 profile 失败: " + e.message)
                    withContext(Dispatchers.Main) {
                        LoadingDialog.hide()
                        showExceptionToast("节点初始化失败：" + (e.message ?: ""))
                    }
                }
            }
            return
        }

        _log("startClash hasProfile -> startServer")
            // ===== 订阅自动更新：超过 12 小时就静默拉新 =====
            try {
                val intervalMs = 12L * 3600 * 1000
                val lastUpdate = activeProfile2.updatedAt
                val now = System.currentTimeMillis()
                val diffMs = if (lastUpdate > 0) now - lastUpdate else Long.MAX_VALUE
                if (diffMs > intervalMs) {
                    _log("startClash 自动更新订阅（距上次 " + (diffMs / 3600000) + " 小时）")
                    withProfile {
                        try {
                            commit(activeProfile2.uuid) { }
                            _log("startClash 订阅已自动更新")
                        } catch (e: Exception) {
                            _log("startClash 订阅更新失败：" + e.message)
                        }
                    }
                } else {
                    _log("startClash 订阅还在缓存期（" + (diffMs / 3600000) + " 小时）")
                }
            } catch (e: Exception) {
                _log("startClash 自动更新异常：" + e.message)
            }
            // ===== 自动更新结束 =====
            withProfile {
                activeProfile2.uuid
            }
            startServer()
        }


    }


    suspend fun withProcessing(executeTask: suspend (suspend (FetchStatus) -> Unit) -> Unit) {
        try {


            executeTask {
                    // applyFrom(it)
                if (it.action == FetchStatus.Action.Verifying) {
                    //验证通过
                    CoroutineScope(Dispatchers.IO).launch {
                        //选中节点
                        activeProfile()
                        //自动开启订阅
                        startServer()
                    }
                }else{
                    //请求中

                }
            }
        } finally {

        }
    }

    private  suspend fun startServer(){
        _log("startServer in")

        val vpnRequest = startClashService()
        _log("startServer vpnRequest=" + vpnRequest)

        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    vpnRequest
                )
                _log("startServer vpn perm result=" + result.resultCode)

                if (result.resultCode == RESULT_OK) {
                    _log("startServer vpn granted")
                    startClashService()
                    withContext(Dispatchers.Main) {
                        LoadingDialog.hide()
                    }
                } else {
                    _log("startServer vpn DENIED, abort")
                    Remote.broadcasts.setConnectionState(com.github.kr328.clash.design.state.ConnectionState.Failed)
                    this@MainActivity.launch { try { design?.setConnectionState(com.github.kr328.clash.design.state.ConnectionState.Failed) } catch (_: Throwable) {} }
                    withContext(Dispatchers.Main) {
                        LoadingDialog.hide()
                    }
                    return
                }
            } else {
                _log("startServer vpn already granted")
            }

            CoroutineScope(Dispatchers.Main).launch {
                withClash {
                    _log("startServer withClash begin")
                    val override = queryOverride(Clash.OverrideSlot.Session)

                    if (PreferenceManager.modeName == TunnelState.Mode.Global.name){
                        override.mode = TunnelState.Mode.Global
                    }else{
                        override.mode = TunnelState.Mode.Rule
                    }
                    patchOverride(Clash.OverrideSlot.Session, override)

                    val names = queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
                    if (names.size > 0) {
                        var progxys = queryProxyGroup(names[0], ProxySort.Default)

                        if (progxys.proxies.isNotEmpty() && progxys.proxies.first().title.length > 0) {
                            if (progxys.proxies.size > 2) {
                                val zutuolist = progxys.proxies.filter { it.title == "自动选择" }
                                var autoNode = ""
                                if (PreferenceManager.selectnodeName.length > 0) {
                                    autoNode = PreferenceManager.selectnodeName
                                } else {
                                    if (zutuolist.size > 0) {
                                        autoNode = "自动选择"
                                    } else {
                                        autoNode = progxys.proxies.firstOrNull()?.title ?: ""
                                    }
                                    PreferenceManager.selectnodeName = autoNode
                                }
                                names.firstOrNull()?.let { patchSelector(it, autoNode) }
                                withContext(Dispatchers.Main) {
                                    design?.setSelectNodeName(PreferenceManager.selectnodeName)
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        LoadingDialog.hide()
                    }
                    _log("startServer withClash end")
                }
            }

        } catch (e: Exception) {
            _log("startServer exception: " + e.message)
            Remote.broadcasts.setConnectionState(com.github.kr328.clash.design.state.ConnectionState.Failed)
            this@MainActivity.launch { try { design?.setConnectionState(com.github.kr328.clash.design.state.ConnectionState.Failed) } catch (_: Throwable) {} }
            withContext(Dispatchers.Main) {
                LoadingDialog.hide()
            }
        }
    }


    private fun _log(msg: String) {
        val line = "${java.util.Date()} $msg"
        try {
            val d = java.io.File(getExternalFilesDir(null), "crash"); d.mkdirs()
            java.io.File(d, "main.txt").appendText("$line\n")
        } catch (_: Exception) {}
        try {
            Thread {
                try {
                    val base = com.github.kr328.clash.network.ApiClientConfig.activeBaseUrl
                    val url = base.trimEnd('/') + "/log/report"
                    val json = org.json.JSONObject().apply {
                        put("tag", "main")
                        put("line", line.take(2000))
                    }.toString()
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Throwable) {}
            }.apply { isDaemon = true }.start()
        } catch (_: Throwable) {}
    }

    private suspend fun activeProfile(){
        withProfile {
            queryAll().firstOrNull()?.let { setActive(it) }
        }

    }
    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName// + "\n" + Bridge.nativeCoreVersion().replace("_", "-")

        }
    }


    private fun requestNotificationPermission() {
        try {
            if (android.os.Build.VERSION.SDK_INT < 33) return
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    0x1234
                )
            }
        } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        if (!lastForeground) {
            lastForeground = true
            startTrafficPolling()
        }
        // 回前台刷运行状态
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) { Remote.broadcasts.refreshRunningState() }
                design?.setClashRunning(Remote.broadcasts.clashRunning)
            } catch (_: Throwable) {}
        }
        // 从"支付成功页"回来 → 强制重拉订阅 → 首页卡片立刻更新
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.delay(300)
                withContext(Dispatchers.Main) {
                    try { performSubscrite() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onPause() {
        super.onPause()
        lastForeground = false
        trafficTimer?.cancel()
        trafficTimer = null
    }

    private fun startTrafficPolling() {
        // 不再每 30 秒轮询订阅：订阅自动更新已由 startClash 里 12 小时机制接管
        trafficTimer?.cancel()
        trafficTimer = null
    }
}