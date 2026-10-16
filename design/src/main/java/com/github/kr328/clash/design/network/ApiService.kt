package com.github.kr328.clash.network

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.io.Serializable
import java.util.concurrent.TimeoutException

/// 所有网络接口
interface ApiService {

    @GET("config")
    suspend fun getConfig(): Response<ConfigResponse>

    @POST("passport/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("passport/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<SendCodeResponse>

    @GET("auth/captcha")
    suspend fun getCaptcha(): Response<CaptchaResponse>

    @POST("auth/captcha/check")
    suspend fun checkCaptcha(@Body request: Map<String, Any>): Response<CaptchaCheckResponse>

    @POST("passport/auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("user/info")
    suspend fun getUserData(@Header("Authorization") auth_data: String): Response<UserInfoResponse>

    @POST("user/update-nickname")
    suspend fun updateNickname(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<PublicResponse>

    @POST("user/update-avatar")
    suspend fun updateAvatar(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<PublicResponse>

    @POST("user/update-password")
    suspend fun updatePassword(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<PublicResponse>


    @POST("user/wallet/submit")
    suspend fun submitWallet(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<PublicResponse>

    @GET("user/wallet/info")
    suspend fun getWalletInfo(@Header("Authorization") auth_data: String): Response<WalletInfoResponse>

    @POST("user/withdraw/submit")
    suspend fun submitWithdraw(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<PublicResponse>

    @GET("user/withdraw/list")
    suspend fun getWithdrawList(@Header("Authorization") auth_data: String): Response<WithdrawListResponse>

    @GET("user/ip")
    suspend fun getIP(@Header("Authorization") auth_data: String): Response<IPResponse>
    @GET("user/getSubscribe")
    suspend fun getSubscribe(@Header("Authorization") auth_data: String): Response<SubscribeResponse>

    @GET("user/plan/fetch")
    suspend fun getplans(@Header("Authorization") auth_data: String): Response<PlansResponse>

    @GET("user/order/fetch")
    suspend fun getOrders(@Header("Authorization") auth_data: String): Response<OrdersResponse>

    @POST("user/order/save") //period=half_year_price&plan_id=2&coupon_code=
    suspend fun saveOrder(@Header("Authorization") auth_data: String,@Body request: SaveOrderRequest): Response<SubmitOrderResponse>

    @GET("user/order/detail") //period=half_year_price&plan_id=2&coupon_code=
    suspend fun getOrderdetail(@Header("Authorization") auth_data: String, @Query("trade_no") trade_no: String): Response<QueryOrderResponse>

    @POST("user/coupon/check")
    suspend fun checkCoupon(@Header("Authorization") auth_data: String, @Body request: CouponCheckRequest): Response<CouponCheckResponse>

    @GET("user/order/getPaymentMethod")
    suspend fun getPaymentList(@Header("Authorization") auth_data: String): Response<PaymentResponse>

    @POST("user/order/checkout")
    suspend fun checkoutOrder(@Header("Authorization") auth_data: String,@Body request: CheckoutrderRequest): Response<PublicResponse>

    @POST("user/order/pay")
    suspend fun payOrder(@Header("Authorization") auth_data: String, @Body request: PayOrderRequest): Response<PayOrderResponse>

    @GET("user/order/query")
    suspend fun queryOrderPay(@Header("Authorization") auth_data: String, @Query("trade_no") trade_no: String): Response<QueryPayResponse>

    @GET("user/invite/fetch")
    suspend fun getinviteList(@Header("Authorization") auth_data: String) :Response<InviteResponse>

    @GET("user/invite/save")
    suspend fun saveinvite(@Header("Authorization") auth_data: String) :Response<SaveInviteCodeResponse>

    @GET("user/invite/customers")
    suspend fun getinviteCustomers(@Header("Authorization") auth_data: String) :Response<InviteCustomersResponse>

    @GET("user/ticket/fetch")
    suspend fun getTickets(@Header("Authorization") auth_data: String): Response<TicketsResponse>

    @GET("user/ticket/fetch")
    suspend fun getTicketsByTID(@Header("Authorization") auth_data: String, @Query("id") ticketID: Int): Response<TicketDetailResponse>

    @POST("user/ticket/close")
    suspend fun closeTicket(@Header("Authorization") auth_data: String,@Body request: Map<String, Int>): Response<PublicResponse>

    @POST("user/ticket/save")
    suspend fun saveTicket(@Header("Authorization") auth_data: String,@Body request: MutableMap<String, String>): Response<PublicResponse>

    @POST("user/ticket/reply")
    suspend fun replyTicket(@Header("Authorization") auth_data: String,@Body request: MutableMap<String, String>): Response<PublicResponse>

    @POST("user/redeem")
    suspend fun redeemCode(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<RedeemResponse>

    @POST("user/chat/send")
    suspend fun chatSend(@Header("Authorization") auth_data: String, @Body request: Map<String, String>): Response<ChatSendResponse>

    @GET("user/chat/history")
    suspend fun chatHistory(@Header("Authorization") auth_data: String, @Query("since") since: Long): Response<ChatHistoryResponse>

    @GET("user/notices")
    suspend fun getNotices(@Header("Authorization") auth_data: String): Response<NoticesResponse>

    @GET("user/bot/status")
    suspend fun getBotStatus(@Header("Authorization") auth_data: String): Response<BotStatusResponse>

    @POST("passport/oauth/github/exchange")
    suspend fun oauthGithubExchange(@Body request: Map<String, String>): Response<OauthGithubResponse>

}


data class SaveInviteCodeResponse(
    val `data`: Any?,

    val message: String?,
    val status: String?
)

data class TicketDetailResponse(
    val `data`: TicketDetailData?,
    val message: String?,
    val status: String?
)

data class TicketDetailData(
    val created_at: Int?,
    val id: Int?,
    val level: Int?,
    val message: List<TicketMessage>?,
    val reply_status: Int?,
    val status: Int?,
    val subject: String?,
    val updated_at: Int?,
    val user_id: Int?
)

data class TicketMessage(
    val created_at: Long?,
    val id: Int?,
    val is_me: Boolean?,
    val message: String?,
    val ticket_id: Int?,
    val updated_at: Long?
)

data class TicketsResponse(
    val `data`: List<TicketsData>?,
    val message: String?,
    val status: String?
)

data class TicketsData(
    val created_at: Long?,
    val id: Int?,
    val level: Int?,
    val message: Any?,
    val reply_status: Int?,
    val status: Int?,
    val subject: String?,
    val updated_at: Long?,
    val user_id: Int?
)


suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<T>
): T? {
    return try {
        val response = withTimeout(13000) {
            apiCall()
        }

        if (response.isSuccessful) {
            response.body()
        } else {
            println("请求失败，错误码: ${response.code()}")
            null
        }
    } catch (e: TimeoutException) {
        println("请求超时，请检查网络连接: ${e.message}")
        null
    } catch (e: IOException) {
        println("网络错误，请检查网络连接: ${e.message}")
        null
    } catch (e: Exception) {
        println("未知错误: ${e.message}")
        null
    }
}



suspend fun <T> safeApiRequestCall(
    apiCall: suspend () -> Response<T>
): Response<T>? {
    return try {
        val response = withTimeout(13000) {
            apiCall()
        }
        if (response.isSuccessful) {
            response
        } else {
            println("请求失败，错误码: ${response.code()}")
            response
        }
    } catch (e: TimeoutException) {
        println("请求超时，请检查网络连接: ${e.message}")
        null
    } catch (e: IOException) {
        println("网络错误，请检查网络连接: ${e.message}")
        null
    } catch (e: Exception) {
        println("未知错误: ${e.message}")
        null
    }
}





data class InviteCustomersResponse(
    val `data`: InviteCustomersData?,
    val message: String?,
    val status: String?
)

data class InviteCustomersData(
    val list: List<InviteCustomer>?,
    val rate: Int?
)

data class InviteCustomer(
    val nickname: String?,
    val short_id: Int?,
    val created_at: Long?,
    val charge: Long?,
    val commission: Long?
)

data class IPResponse(
    val `data`: IPData?,
    val message: String?,
    val status: String?
)

data class IPData(
    val ip: String?,
    val country: String?,
    val region: String?,
    val city: String?,
    val isp: String?,
    val org: String?,
    val `as`: String?,
    val mobile: Boolean?,
    val proxy: Boolean?,
    val hosting: Boolean?
)

data class InviteResponse(
    val `data`: InviteData?,
    val error: Any?,
    val message: String?,
    val status: String?
)

data class InviteData(
    val codes: List<InviteCode>?,
    val stat: List<Int>?
)

data class InviteCode(
    val code: String?,
    val created_at: Int?,
    val pv: Int?,
    val status: Int?,
    val updated_at: Int?,
    val user_id: Int?
)
data class CheckoutrderRequest(
    val trade_no: String,
    val method: Int
)

data class PaymentResponse(
    val `data`: List<PaymentData>?,
    val message: String?,
    val status: String?
)

data class PaymentData(
    val handling_fee_fixed: Any?,
    val handling_fee_percent: String?,
    val icon: Any?,
    val id: Int?,
    val name: String?,
    val payment: String?
)
//
data class  QueryOrderRequest(
    val  trade_no: String
)


data class QueryOrderResponse(
    val `data`: QueryOrderData?,
    val message: String?,
    val status: String?
)

data class QueryOrderData(
    val actual_commission_balance: Any?,
    val balance_amount: Any?,
    val callback_no: Any?,
    val commission_balance: Int?,
    val commission_status: Int?,
    val coupon_code: Any?,
    val coupon_id: Any?,
    val created_at: Int?,
    val discount_amount: Any?,
    val handling_amount: Int?,
    val id: Int?,
    val invite_user_id: Any?,
    val paid_at: Any?,
    val payment_id: Int?,
    val period: String?,
    val plan: PlanData?,
    val plan_id: Int?,
    val refund_amount: Any?,
    val site_id: Any?,
    val status: Int?,
    val surplus_amount: Any?,
    val surplus_order_ids: Any?,
    val tixianstatus: Any?,
    val total_amount: Int?,
    val trade_no: String?,
    val try_out_plan_id: Int?,
    val type: Int?,
    val updated_at: Int?,
    val user_id: Int?
){
    // status 对应的中文描述
    val statusZh: String
        get() = when (status) {
            0 -> "待支付"
            1 -> "已支付"
            2 -> "已取消"
            3 -> "已支付"
            else -> "未知"
        }
    // period 对应的中文描述
    val periodZh: String
        get() = when (period) {
            "month_price" -> "月付"
            "quarter_price" -> "季付"
            "half_year_price" -> "半年付"
            "year_price" -> "年付"
            "two_year_price" -> "两年付"
            "three_year_price" -> "三年付"
            "onetime_price" -> "一次性付"
            else -> ""
        }
}

data class SaveOrderRequest(
    val period: String,
    val plan_id: Int,
    val coupon_code: String?,
    val payment: String = "alipay"
)


data class CouponCheckRequest(
    val code: String,
    val plan_id: Int,
    val period: String
)

data class CouponCheckData(
    val valid: Boolean? = null,
    val reason: String? = null,
    val type: String? = null,
    val value: Int? = null,
    val original_amount: Long? = null,
    val discounted_amount: Long? = null
)

data class CouponCheckResponse(
    val data: CouponCheckData?,
    val message: String?,
    val status: String?
)


data class PublicResponse(
    val data: String?,
    val message: String?,
    val status: String?
)


data class ChatMessageData(
    val id: Int? = null,
    val ticket_id: Int? = null,
    val sender: String? = null,
    val content: String? = null,
    val created_at: Long? = null
)

data class ChatSendData(
    val room_id: Int? = null,
    val created_at: Long? = null
)

data class ChatSendResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: ChatSendData? = null
)

data class ChatHistoryData(
    val room_id: Int? = null,
    val messages: List<ChatMessageData>? = null
)

data class ChatHistoryResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: ChatHistoryData? = null
)

data class RedeemData(
    val reward_gb: Double? = null,
    val reward_days: Int? = null
)

data class RedeemResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: RedeemData? = null
)

data class NoticeData(
    val id: Int? = null,
    val type: String? = null,
    val title: String? = null,
    val content: String? = null,
    val created_at: Long? = null,
    val read: Boolean? = null
)

data class NoticesResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: List<NoticeData>? = null
)

data class SubmitOrderResponse(
    val data: String?,
    val message: String?,
    val status: String?
)


data class BannerData(
    val id: Int? = null,
    val type: String? = null,
    val media: String? = null,
    val image_url: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val link: String? = null,
    val sort: Int? = null
)

data class ConfigEnvelope(
    val code: Int = 0,
    val message: String? = null,
    val data: ConfigResponse? = null
)

data class ConfigResponse (
    val baseURL: String = "",
    @SerializedName("baseDURL") val baseDYURL: String = "",
    val mainregisterURL: String = "",
    val paymentURL: String = "",
    @SerializedName("telegramUrl") val telegramurl: String = "",
    @SerializedName("kefuUrl") val kefuurl: String = "",
    val websiteURL: String = "",
    val crisptoken: String = "",
    val banners: List<BannerData>? = null,
    val message: String = "",
    val code: Int = 1
)

data class WalletInfoResponse(
    val data: WalletInfoData?,
    val message: String?,
    val status: String?
)
data class WalletInfoData(
    val real_name: String?,
    val usdt_addr: String?,
    val alipay: String?,
    val wallet_status: Int?
)

data class WithdrawListResponse(
    val data: WithdrawListData?,
    val message: String?,
    val status: String?
)
data class WithdrawListData(
    val list: List<WithdrawItem>?,
    val balance: Long? = 0,
    val total: Long? = 0,
    val withdrawn: Long? = 0,
    val notif: WithdrawNotif? = null
)
data class WithdrawNotif(
    val id: Int?,
    val amount: Long?,
    val status: Int?
)
data class WithdrawItem(
    val id: Int?,
    val amount: Long?,
    val method: String?,
    val status: Int?,
    val created_at: Long?
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val code: String,
    val invite_code: String = ""
)

data class SendCodeRequest(val email: String)

data class CaptchaData(
    val id: String? = null,
    val image: String? = null,
    val hint: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

data class CaptchaResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: CaptchaData? = null
)

data class CaptchaCheckData(
    val pass: Boolean? = null
)

data class CaptchaCheckResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: CaptchaCheckData? = null
)

data class SendCodeResponse(
    val message: String?,
    val data: SendCodeData?
)

data class SendCodeData(val msg: String?)
data class RegisterResponse(val success: Boolean, val message: String)

data class LoginRequest(val email: String, val password: String ,val captchaData: String = "")

data class LoginResponse(val message: String?, val data: LoginData?)


data class LoginData (
    val token: String?,
    val isAdmin: Int?,
    val auth_data: String?
)



data class SubscribeResponse (
    val data: SubscribeData
)


data class SubscribeData  (

    @SerializedName("short_id") val short_id : Int? = 0,
    @SerializedName("plan_id") val plan_id : Int?,
    @SerializedName("token") val token : String,
    @SerializedName("expired_at") val expired_at : Long?,
    @SerializedName("u") val u : Long,
    @SerializedName("d") val d : Long,
    @SerializedName("transfer_enable") val transfer_enable : Long?,
    @SerializedName("email") val email : String,
    @SerializedName("uuid") val uuid : String,
    @SerializedName("plan") val plan : PlanData?,
    @SerializedName("subscribe_url") val subscribe_url : String,
    @SerializedName("reset_day") val reset_day : String?

)

data class PlanData (
    @SerializedName("id") val id : Int,
    @SerializedName("group_id") val group_id : Int?,
    @SerializedName("transfer_enable") val transfer_enable : Long?,
    @SerializedName("name") val name : String,
    @SerializedName("speed_limit") val speed_limit : Long?,
    @SerializedName("show") val show : Int?,
    @SerializedName("sort") val sort : Int?,
    @SerializedName("renew") val renew : Int?,
    @SerializedName("content") val content : String,
    @SerializedName("month_price") val month_price : Long?,
    @SerializedName("quarter_price") val quarter_price : Long?,
    @SerializedName("half_year_price") val half_year_price : Long?,
    @SerializedName("year_price") val year_price : Long?,
    @SerializedName("two_year_price") val two_year_price : Long?,
    @SerializedName("three_year_price") val three_year_price : Long?,
    @SerializedName("onetime_price") val onetime_price : Int?,
    @SerializedName("reset_price") val reset_price : Long?,
    @SerializedName("reset_traffic_method") val reset_traffic_method : String?,
    @SerializedName("capacity_limit") val capacity_limit : Long?,
    @SerializedName("created_at") val created_at : Long?,
    @SerializedName("updated_at") val updated_at : Long?,
    @SerializedName("tagline") val tagline : String? = null,
    @SerializedName("speed") val speed : String? = null,
    @SerializedName("hot") val hot : Int? = 0,
    @SerializedName("features") val features : List<String>? = null
) : Serializable



data class UserInfoResponse (
    val data: UserInfoData
)

data class UserInfoData (
    val email: String,
    val transferEnable: Long?,
    val lastLoginAt: Long,
    val createdAt: Long?,
    val banned: Long?,
    val remindExpire: Long?,
    val remindTraffic: Long?,
    val expiredAt: Long?,
    val balance: Long?,
    val commissionBalance: Long?,
    val uuid: String,
    val avatarURL: String?,
    val shortId: Int? = 0,
    val nickname: String? = ""
)

 data class PlansResponse(
    val data: List<PlanData>?,

    val message: String?,
    val status: String?
)


data class OrdersResponse(
    val `data`: List<OrderData>?,

    val message: String?,
    val status: String?
)

data class OrderData(
    val actual_commission_balance: Any?,
    val balance_amount: Any?,
    val callback_no: Any?,
    val commission_balance: Int?,
    val commission_status: Int?,
    val coupon_code: Any?,
    val coupon_id: Any?,
    val created_at: Long?,
    val discount_amount: Any?,
    val handling_amount: Int?,
    val invite_user_id: Any?,
    val paid_at: Any?,
    val payment_id: Int?,
    val period: String?,
    val plan: PlanData?,
    val plan_id: Int?,
    val refund_amount: Any?,
    val site_id: Any?,
    val status: Int?,
    val surplus_amount: Any?,
    val surplus_order_ids: Any?,
    val tixianstatus: Any?,
    val total_amount: Int?,
    val trade_no: String?,
    val type: Int?,
    val updated_at: Long?
){
    // status 对应的中文描述
    val statusZh: String
        get() = when (status) {
            0 -> "待支付"
            1 -> "已支付"
            2 -> "已取消"
            3 -> "已支付"
            else -> "未知"
        }

    // period 对应的中文描述
    val periodZh: String
        get() = when (period) {
            "month_price" -> "月付"
            "quarter_price" -> "季付"
            "half_year_price" -> "半年付"
            "year_price" -> "年付"
            "two_year_price" -> "两年付"
            "three_year_price" -> "三年付"
            "onetime_price" -> "一次性付"
            else -> ""
        }
}


data class PayOrderRequest(
    val trade_no: String,
    val method: String
)

data class PayOrderResponse(
    val code: Int = 0,
    val msg: String? = null,
    val data: PayOrderData? = null
)

data class PayOrderData(
    val pay_info: String? = null,
    val qrcode: String? = null,
    val payurl: String? = null,
    val trade_no: String? = null,
    val amount: String? = null
)

data class QueryPayResponse(
    val code: Int = 0,
    val msg: String? = null,
    val data: QueryPayData? = null
)

data class QueryPayData(
    val paid: Boolean = false
)


data class BotStatusData(
    val short_id: Int? = null,
    val bound: Boolean? = null,
    val tg_id: String? = null,
    val last_sign: String? = null,
    val traffic_mb: Long? = null,
    val mivra_url: String? = null,
    val guide: String? = null
)

data class OauthGithubData(
    val token: String? = null,
    val short_id: Int? = null,
    val email: String? = null,
    val nickname: String? = null
)

data class OauthGithubResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: OauthGithubData? = null
)

data class BotStatusResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: BotStatusData? = null
)
