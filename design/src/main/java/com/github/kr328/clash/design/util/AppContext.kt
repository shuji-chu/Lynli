package com.github.kr328.clash.design.util

import android.content.Context

object AppContext {
    lateinit var ctx: Context
        private set

    fun init(c: Context) {
        ctx = c.applicationContext
    }
}
