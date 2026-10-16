package com.github.kr328.clash.network

import android.content.Context
import com.github.kr328.clash.design.PreferenceManager
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
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
        .addInterceptor { chain ->
            val req = chain.request()
            if (req.method != "GET") return@addInterceptor chain.proceed(req)
            var lastEx: java.io.IOException? = null
            for (i in 0..2) {
                try {
                    return@addInterceptor chain.proceed(req)
                } catch (e: java.io.IOException) {
                    lastEx = e
                    if (i < 2) try { Thread.sleep(300L * (i + 1)) } catch (_: InterruptedException) {}
                }
            }
            throw lastEx ?: java.io.IOException("request failed after retries")
        }
        .build()

    private val configURL: String
        get() {
            var url = PreferenceManager.baseURL
            if (url.isNullOrEmpty()) return ApiClientConfig.activeBaseUrl
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
            url = url.trimEnd('/')
            return if (url.endsWith("/api/v1")) "$url/" else "$url/api/v1/"
        }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(configURL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }
}


object ApiClientConfig {
    // 订阅转接（不需要就留空）
    const val ConfigNodeURL = ""

    // ====== 域名候选（第一个通就用，顺序=优先级）======
    // 换域名 / 挂域名，改这一处，重新打包即可
    private val HOSTS = listOf(
        "https://lynli.kz.ci",              // 主
        "https://api.lynli.me",             // 备1（Cloudflare Worker，配好自动生效）
        "https://api.yisoujiasu.de5.net"    // 备2
    )

    @Volatile
    var activeBaseUrl: String = HOSTS[0] + "/api/v1/"
        private set

    // 后台线程探测一次（不阻塞主线程）
    init {
        Thread {
            detect()
        }.apply { isDaemon = true }.start()
    }

    private fun detect() {
        for (h in HOSTS) {
            try {
                val req = okhttp3.Request.Builder().url("$h/api/v1/config").get().build()
                val probe = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build()
                probe.newCall(req).execute().use { resp ->
                    if (resp.code in 200..499) {
                        activeBaseUrl = "$h/api/v1/"
                        return
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // 每次创建都读 activeBaseUrl（探测完自动切）
    val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(activeBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
}
