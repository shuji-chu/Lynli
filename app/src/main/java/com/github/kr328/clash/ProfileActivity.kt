package com.github.kr328.clash
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.ProfileDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showCustomDialog
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.utity.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.lifecycle.lifecycleScope


class ProfileActivity  : BaseActivity<ProfileDesign>()  {

    private var subscribeJob: kotlinx.coroutines.Job? = null


    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName //+ "\n" + Bridge.nativeCoreVersion().replace("_", "-")

        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override suspend fun main() {
        PreferenceManager.init(this)

        val design = ProfileDesign(this)
        setContentDesign(design)

        try { design.requestdataDisible() } catch (e: Throwable) { }

        // 30 秒自动刷新订阅数据（实时）
        subscribeJob?.cancel()
        subscribeJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000)
                try {
                    withContext(Dispatchers.Main) { design.requestdataDisible() }
                } catch (_: Throwable) {}
            }
        }

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))
        design.queryVersion(queryAppVersionName())
        while (isActive) {
            select<Unit> {
                events.onReceive {
                    println("events  : " + it.name)
                }
                design.requests.onReceive {
                    println("requests  : " + it.name)
                    when (it) {

                        ProfileDesign.Request.OpenBuyPlan -> startActivity(PlansActivity::class.intent)
                        ProfileDesign.Request.OpenMyOrders -> startActivity(MyOrdersActivity::class.intent)
                        ProfileDesign.Request.OpenMyBlance -> startActivity(SubmitOrderActivity::class.intent)
                        ProfileDesign.Request.OpenMyInvites ->startActivity(InvitationActivity::class.intent)
                        ProfileDesign.Request.OpenAccount -> startActivity(
                            android.content.Intent(this@ProfileActivity, AccountActivity::class.java)
                        )
                        ProfileDesign.Request.OpenFaq -> startActivity(
                            android.content.Intent(this@ProfileActivity, FaqActivity::class.java)
                        )
                        ProfileDesign.Request.OpenBusiness -> startActivity(
                            android.content.Intent(this@ProfileActivity, BusinessActivity::class.java)
                        )
                        ProfileDesign.Request.OpenCustomView -> {
                            try {
                                val _cfg = com.github.kr328.clash.design.PreferenceManager.getConfigFromPreferences(this@ProfileActivity)
                                val _url = if (!_cfg?.mivraurl.isNullOrEmpty()) _cfg!!.mivraurl!! else "https://m.mivra.im/SOCKS5"
                                val _i = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(_url)
                                )
                                startActivity(_i)
                            } catch (_: Throwable) {
                                android.widget.Toast.makeText(this@ProfileActivity, "无法打开签到群，请手动前往", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                            /*try {


                                withContext(Dispatchers.Main) {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                                    intent.setData(Uri.parse(PreferenceManager.getConfigFromPreferences(this@ProfileActivity)?.telegramurl))
                                    startActivity(intent)
                                }
                            } catch (e: Exception) {
                                design.showToast("当前手机未安装浏览器",ToastDuration.Long)

                            } */

                        ProfileDesign.Request.OpenLogout -> {
                            showCustomDialog(
                                title = "提示",
                                message = "确定要退出吗？",
                                positiveButtonText = "确定",
                                negativeButtonText = "取消",
                                onPositiveClick = {
                                    LoadingDialog.show(this@ProfileActivity, "正在退出...")
                                    // 执行确认操作
                                    stopClashService()
                                    // 清理缓存，清除节点信息，切换界面
                                    launch {
                                        withProfile {
                                            var allProfiles = queryAll()
                                            println(allProfiles)
                                            if (allProfiles.isNotEmpty() && allProfiles.count() > 0) {
                                                allProfiles.forEach {
                                                    delete(it.uuid)
                                                    println("删除节点：${it.source}")
                                                }
                                                //删除所有节点
                                            }
                                        }
                                    }
                                    PreferenceManager.clearData()

                                    //2s 后执行
// 在协程作用域中执行
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        delay(2000)  // 协程延迟2000毫秒（2秒）
                                        println("协程中的延迟执行")
                                        LoadingDialog.hide()
                                        startActivity(LoginActivity::class.intent)
                                        finish()

                                    }

                                },
                                onNegativeClick = {
                                    // 执行取消操作
                                }
                            )

                        }
                        ProfileDesign.Request.OpenGSettings -> startActivity(
                            android.content.Intent(this@ProfileActivity, NewSettingsActivity::class.java)
                        )//  startActivity(NetworkSettingsActivity::class.intent)
                        ProfileDesign.Request.OpenMyGongdan ->startActivity(GongdanActivity::class.intent)
                        ProfileDesign.Request.OpenCustomViewIPtest -> startActivity(android.content.Intent(this@ProfileActivity, IpQueryActivity::class.java))
                        ProfileDesign.Request.OpenCustomViewSpeed ->  startActivity(H5WebActivity::class.intent.putExtra("url","https://fast.com/"))
                        ProfileDesign.Request.OpenTabHome -> {
                            startActivity(android.content.Intent(this@ProfileActivity, MainActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            })
                        }
                        ProfileDesign.Request.OpenTabPlan -> {
                            startActivity(android.content.Intent(this@ProfileActivity, PlansActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            })
                        }
                        ProfileDesign.Request.OpenTabMine -> { /* 已在本页 */ }
                        ProfileDesign.Request.OpenRedeem -> startActivity(android.content.Intent(this@ProfileActivity, RedeemActivity::class.java))
                    }
                }
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        try { subscribeJob?.cancel() } catch (_: Throwable) {}
        subscribeJob = null
    }
}
