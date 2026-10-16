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
import java.util.concurrent.CountDownLatch


object ApiClient {
    private val INFLIGHT = HashMap<String, ByteArray>()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // 请求去重：GET 3 秒内同 URL 只发一次（缓存字节，命中时构造新 Response）
        .addInterceptor { chain ->
            val req = chain.request()
            val key = req.method + "|" + req.url.toString()
            val map = INFLIGHT

            // 命中：构造全新 Response（body 用缓存字节）
            if (req.method == "GET") {
                val cached = synchronized(map) { map[key] }
                if (cached != null) {
                    return@addInterceptor okhttp3.Response.Builder()
                        .request(req)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(cached.toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }

            val resp = chain.proceed(req)

            // 只缓存 GET 200 的 body 字节
            if (req.method == "GET" && resp.code == 200) {
                val bytes = try { resp.body?.bytes() } catch (_: Throwable) { null }
                if (bytes != null) {
                    synchronized(map) {
                        map[key] = bytes
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            synchronized(map) { map.remove(key) }
                        }, 3000)
                    }
                    // 原 body 已被读掉，用 bytes 重建
                    return@addInterceptor resp.newBuilder()
                        .body(bytes.toResponseBody(resp.body?.contentType()))
                        .build()
                }
            }
            resp
        }
        .apply {
            try {
                val ctx = com.github.kr328.clash.design.util.AppContext.getOrNull()
                if (ctx != null) {
                    cache(okhttp3.Cache(java.io.File(ctx.cacheDir, "http_cache"), 10L * 1024 * 1024))
                }
            } catch (_: Throwable) {}
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
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
        // 所有方法（GET + POST）重试 3 次，指数退避
        .addInterceptor { chain ->
            val req = chain.request()
            var lastEx: java.io.IOException? = null
            var lastResp: okhttp3.Response? = null
            for (i in 0..2) {
                try {
                    val resp = chain.proceed(req)
                    // 5xx 也重试
                    if (resp.code in 500..599 && i < 2) {
                        lastResp = resp
                        try { Thread.sleep(400L * (i + 1)) } catch (_: InterruptedException) {}
                        continue
                    }
                    return@addInterceptor resp
                } catch (e: java.io.IOException) {
                    lastEx = e
                    if (i < 2) try { Thread.sleep(400L * (i + 1)) } catch (_: InterruptedException) {}
                }
            }
            lastResp ?: throw (lastEx ?: java.io.IOException("request failed after retries"))
        }
        .build()

    private val configURL: String
        get() {
            // 优先用已探测好的
            if (ApiClientConfig.isReady()) return ApiClientConfig.activeBaseUrl
            // 其次用用户上次保存的
            var url = PreferenceManager.baseURL
            if (!url.isNullOrEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
                url = url.trimEnd('/')
                return if (url.endsWith("/api/v1")) "$url/" else "$url/api/v1/"
            }
            // 最后 fallback 到默认
            return ApiClientConfig.activeBaseUrl
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
    const val ConfigNodeURL = ""

    // ====== 域名候选（CF 优先，国内最稳）======
    private val HOSTS = listOf(
        "https://api.lynli.me",             // CF 反代（国内最稳）
        "https://lynli.kz.ci",              // 主
        "https://api.yisoujiasu.de5.net"    // 备用
    )

    @Volatile
    private var _activeBaseUrl: String = HOSTS[0] + "/api/v1/"

    @Volatile
    private var _ready: Boolean = false

    @Volatile
    private var _latch: CountDownLatch = CountDownLatch(1)

    val activeBaseUrl: String
        get() = _activeBaseUrl

    fun isReady(): Boolean = _ready

    // 启动并发探测（3 个域名同时测，谁先回通用谁），1.5 秒内至少返回一个结果
    init {
        // 先读上次缓存（秒用）
        try {
            val cached = PreferenceManager.cached_active_host
            if (cached.isNotEmpty()) {
                _activeBaseUrl = cached
                _ready = true
            }
        } catch (_: Throwable) {}

        Thread {
            detectParallel()
            _latch.countDown()
        }.apply { isDaemon = true }.start()
    }

    /**
     * 阻塞等待探测完成（最多 2 秒）
     * UI 线程调用前先看 isReady()，没 ready 才等
     */
    fun await(maxMs: Long = 2000) {
        if (_ready) return
        try { _latch.await(maxMs, TimeUnit.MILLISECONDS) } catch (_: Throwable) {}
    }

    private fun detectParallel() {
        val latch = CountDownLatch(HOSTS.size)
        val okUrl = java.util.concurrent.atomic.AtomicReference<String>("")

        for (h in HOSTS) {
            Thread {
                try {
                    val req = okhttp3.Request.Builder().url("$h/api/v1/config").get().build()
                    val probe = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(6, TimeUnit.SECONDS)
                        .readTimeout(6, TimeUnit.SECONDS)
                        .build()
                    probe.newCall(req).execute().use { resp ->
                        if (resp.code in 200..499 && okUrl.get().isEmpty()) {
                            okUrl.compareAndSet("", "$h/api/v1/")
                        }
                    }
                } catch (_: Throwable) {} finally {
                    latch.countDown()
                }
            }.apply { isDaemon = true }.start()
        }

        try { latch.await(8, TimeUnit.SECONDS) } catch (_: Throwable) {}

        val winner = okUrl.get()
        if (winner.isNotEmpty()) {
            _activeBaseUrl = winner
            _ready = true
            try { PreferenceManager.cached_active_host = winner } catch (_: Throwable) {}
            android.util.Log.i("LynliNet", "API host detected: $winner")
        } else {
            _ready = true  // 一个都没通，用默认继续试
            android.util.Log.w("LynliNet", "All hosts unreachable, fallback to ${_activeBaseUrl}")
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(_activeBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
}
