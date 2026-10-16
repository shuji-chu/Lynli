package com.github.kr328.clash.design.util

import android.content.Context

object AppContext {
    private var _ctx: Context? = null

    val ctx: Context
        get() = _ctx ?: throw IllegalStateException("AppContext not initialized")

    fun init(c: Context) {
        _ctx = c.applicationContext
    }

    fun getOrNull(): Context? = _ctx
}
