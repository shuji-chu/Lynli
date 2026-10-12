package com.github.kr328.clash.design

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import androidx.transition.Visibility
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.ConfigOrderDesign.Request
import com.github.kr328.clash.design.databinding.ActivityPaymentItemBinding
import com.github.kr328.clash.design.databinding.ActivityPlanItemBinding
import com.github.kr328.clash.design.databinding.ActivitySubmitorderBinding
import com.github.kr328.clash.design.network.APIGlobalObject
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.design.util.showCustomDialog
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.CheckoutrderRequest
import com.github.kr328.clash.network.PaymentData
import com.github.kr328.clash.network.PayOrderRequest
import com.github.kr328.clash.network.PayOrderResponse
import com.github.kr328.clash.network.QueryPayResponse
import com.github.kr328.clash.network.QueryPayData
import com.github.kr328.clash.network.PublicResponse
import com.github.kr328.clash.network.QueryOrderData
import com.github.kr328.clash.network.QueryOrderRequest
import com.github.kr328.clash.network.SaveOrderRequest
import com.github.kr328.clash.network.SubmitOrderResponse
import com.github.kr328.clash.network.safeApiRequestCall
import com.github.kr328.clash.utity.LoadingDialog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class SubmitOrderViewDesign  (context: Context) : Design<SubmitOrderViewDesign.Request>(context)  {

    enum class Request {
        SubmitPay,
    }


    private  var  currentChoose: PaymentData? = null

    private  var  tradeNo: String = ""


    private val binding = ActivitySubmitorderBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root


    fun request(request: Request) {
        if (request == Request.SubmitPay) {
            val method = if (currentChoose?.payment == "usdt") "usdt" else "alipay"
            LoadingDialog.show(context, "正在创建支付...")
            CoroutineScope(Dispatchers.IO).launch {
                val apiService = ApiClient.retrofit.create(ApiService::class.java)
                safeApiRequestCall {
                    apiService.payOrder(PreferenceManager.loginauthData, PayOrderRequest(tradeNo, method))
                }.let { resp ->
                    withContext(Dispatchers.Main) { LoadingDialog.hide() }
                    val body = resp?.body()
                    if (resp != null && resp.isSuccessful && body?.code == 1) {
                        val d = body.data
                        withContext(Dispatchers.Main) {
                            val payUrl = d?.payurl ?: ""
                            val qrUrl = d?.qrcode ?: ""
                            val info = d?.pay_info ?: ""
                            val shown = payUrl.ifEmpty { qrUrl }

                            if (shown.startsWith("http://") || shown.startsWith("https://")) {
                                val dlg = PaymentWebDialog(context, shown) {
                                    pollPaid(tradeNo, null)
                                }
                                dlg.show()
                                pollPaid(tradeNo, dlg)
                            } else if (qrUrl.startsWith("tron:")) {
                                context.showCustomDialog(
                                    title = "USDT 支付",
                                    message = "请向以下 TRC20 地址转入 " + (d?.amount ?: "") + " USDT：\n\n" + info + "\n\n转账后约 30 秒自动到账。",
                                    positiveButtonText = "我已转账",
                                    negativeButtonText = "取消",
                                    onPositiveClick = { pollPaid(tradeNo, null) },
                                    onNegativeClick = {}
                                )
                            } else {
                                showToast("无法识别的支付返回", ToastDuration.Long)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("创建支付失败：" + (body?.msg ?: "网络错误"), ToastDuration.Long)
                        }
                    }
                }
            }
        }
    }

    private fun pollPaid(tradeNo: String, dialog: PaymentWebDialog?) {
        CoroutineScope(Dispatchers.IO).launch {
            repeat(200) {
                delay(3000)
                try {
                    val apiService = ApiClient.retrofit.create(ApiService::class.java)
                    safeApiRequestCall {
                        apiService.queryOrderPay(PreferenceManager.loginauthData, tradeNo)
                    }.let { r ->
                        if (r?.body()?.data?.paid == true) {
                            withContext(Dispatchers.Main) {
                                try { dialog?.dismiss() } catch (_: Throwable) {}
                                showToast("支付成功，套餐已开通", ToastDuration.Long)
                            }
                            return@launch
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    init {
        binding.self = this
        binding.activityBarLayout.applyFrom(context)

    }

    fun fetchTradeInfo(trade_no: String?){
        tradeNo = trade_no ?: ""
        LoadingDialog.show(context, "请求订单数据...")

        CoroutineScope(Dispatchers.IO).launch {
            val apiService = ApiClient.retrofit.create(ApiService::class.java)
            safeApiRequestCall { apiService.getOrderdetail(PreferenceManager.loginauthData,trade_no?: "")}.let {

                if(it != null && it.isSuccessful){
                    if (it.body()?.data != null){
                        withContext(Dispatchers.Main) {
                            fillData(it.body()?.data)
                            LoadingDialog.hide()
                            // 清空 binding.container 的所有子视图
                            binding.container.removeAllViews()

                        }
                        var selectedIndex = 0

                        //继续获取支付方式
                        safeApiRequestCall { apiService.getPaymentList(PreferenceManager.loginauthData)}.let {
                            if(it != null && it.isSuccessful){
                                println("${it.body()}")
                                withContext(Dispatchers.Main) {
                                    it.body().let {
                                        it?.data.let {
                                            if (it != null) {
                                                for (i in it.indices) {
                                                    // 设置点击事件和单选逻辑
                                                    val frameLayout =
                                                        ActivityPaymentItemBinding.inflate(
                                                            context.layoutInflater,
                                                            context.root,
                                                            false
                                                        )
                                                    val item = it[i]
                                                    frameLayout.typeTextView.text = item.name
                                                    frameLayout.amountTextView.text =  "手续费：${item.handling_fee_percent ?: "0.00"}%"


                                                    frameLayout.planitemFrameLayout.setOnClickListener {
                                                        selectedIndex = i
                                                        currentChoose = item

                                                        for (j in 0 until binding.container.childCount) {
                                                            val child =
                                                                binding.container.getChildAt(j)
                                                            println("selectedIndex: ${selectedIndex}  ${child.background} ")
                                                            child.background =
                                                                ContextCompat.getDrawable(
                                                                    context,
                                                                    if (j == selectedIndex) R.drawable.card_border_selected else R.drawable.card_border
                                                                )
                                                        }
                                                    }
                                                    binding.container.addView(frameLayout.root)



                                                    if (i == 0){
                                                        //默认选中第一个
                                                        currentChoose = it[0]
                                                        //设置手续费显示：

                                                        //binding.configorderPlanFeeAmount =  currentChoose.handling_fee_percent
                                                        frameLayout.planitemFrameLayout.background = ContextCompat.getDrawable(
                                                            context,  R.drawable.card_border_selected
                                                        )

                                                    }



                                                }


                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }else{
                    val errorinfo =  (it?.errorBody()?.string())
                    if (errorinfo != null){
                        //解析错错误信息

                        val gson = Gson()
                        val submitResponse = gson.fromJson(errorinfo, PublicResponse::class.java)

                        if (submitResponse?.message != null){
                            withContext(Dispatchers.Main) {
                                showToast("订单请求失败:${submitResponse.message}", ToastDuration.Long)
                            }
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            showToast("下单失败:请求数据失败", ToastDuration.Long)
                        }
                    }

                }

                withContext(Dispatchers.Main) {

                    LoadingDialog.hide()
                }
            }
        }

    }


    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())  // 定义格式
        val date = Date(timestamp)  // 将时间戳转为 Date 对象
        return dateFormat.format(date)  // 格式化为字符串
    }

    private fun fillData(data: QueryOrderData?) {
        data.let {
            binding.configorderPlanname  = "商品名称：${data?.plan?.name}"
            binding.configorderTradeno = "订单号：${data?.trade_no}"
            binding.configorderPlanname2 = data?.plan?.name
            val amount = (data?.total_amount?.toDouble() ?: 0.0)/100
            binding.configorderPlanamount = "¥ %.2f".format(amount)
            binding.configorderPlantype = "类型/周期：${data?.periodZh}"
            val gb = ((data?.plan?.transfer_enable ?: 0L).toDouble()) / 1024.0 / 1024.0 / 1024.0
            binding.configorderPlantransfer = "商品流量：%.0f GB".format(gb)
            binding.configorderTimer = "创建时间：${ formatTimestamp((data?.created_at ?: 0) * 1000L)}"
            binding.orderDetailsStatus.text = data?.statusZh
            if (data?.status == 0){
                binding.confirmPaymentButton.visibility = View.VISIBLE
            }else{
                binding.confirmPaymentButton.visibility = View.GONE
            }
        }
    }


}