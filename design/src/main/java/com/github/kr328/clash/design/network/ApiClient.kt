package com.github.kr328.clash.network

import android.content.Context
import com.github.kr328.clash.design.PreferenceManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            // 演示模式：所有请求直接返回空 JSON，不联网，不崩
            // Demo mode: return empty JSON for all requests
            if (PreferenceManager.loginToken == "demo-token") {
                val empty = okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{\"data\":null,\"message\":\"demo\",\"status\":\"success\",\"code\":1}".toResponseBody("application/json".toMediaType()))
                    .build()
                return@addInterceptor empty
            }
            chain.proceed(chain.request())
        }
        .build()

    // Lazy initialization of configURL
    private val configURL: String
        get() {
            val url = PreferenceManager.baseURL
            return if (url.isNullOrEmpty()) {
                "https://vungles.com/" // Default fallback URL to prevent crash
            } else {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }
            }
        }


    // Retrofit initialization
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(configURL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }
}

object ApiClientConfig {
        private const val ConfigURL = "https://vungles.com/api/test/" // BASE 请求配置文件地址 见说明文档
//    private const val ConfigURL = "https://uuvpn.oss-cn-hongkong.aliyuncs.com/api/test/" // BASE 请求配置文件地址 见说明文档
    //这个链接主要是为了配置转化，一般有的订阅地址被墙之后国内无法访问，这里就是了防止被墙，使用我们的香港服务器进行顶级域名防护
    //const val ConfigNodeURL = "https://api.xxxx.com/api/parseyamlclash.php?target=clashmeta&url=" //通过香港服务器转接一次Clash转化的订阅地址
    const val ConfigNodeURL = ""

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ConfigURL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
}
