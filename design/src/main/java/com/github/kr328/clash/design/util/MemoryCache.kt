package com.github.kr328.clash.design.util

import android.util.LruCache

/**
 * 全局内存缓存：切页面不重新请求，秒开。
 * key 建议用 "<Class>#<tag>"，value 是 List 或任意数据对象。
 */
object MemoryCache {
    private const val MAX_ENTRIES = 64

    private val cache = object : LruCache<String, Any>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: Any): Int = 1
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = try {
        cache.get(key) as? T
    } catch (_: Throwable) {
        null
    }

    fun put(key: String, value: Any) {
        try { cache.put(key, value) } catch (_: Throwable) {}
    }

    fun remove(key: String) {
        try { cache.remove(key) } catch (_: Throwable) {}
    }

    fun clear() {
        try { cache.evictAll() } catch (_: Throwable) {}
    }
}
