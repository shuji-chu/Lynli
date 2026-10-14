package com.github.kr328.clash

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children

object ThemeHelper {

    // 浅色 → 深色 映射
    private val COLOR_MAP_LIGHT_TO_DARK = mapOf(
        "#EEF4FF" to "#0F1620",   // 页面背景
        "#FFFFFF" to "#1C2430",   // 卡片背景
        "#1F2937" to "#E8EAED",   // 主文字
        "#333333" to "#D0D0D0",   // 正文
        "#64748B" to "#98A2B3",   // 次要文字
        "#94A3B8" to "#7A8494",   // 提示文字
        "#F0F0F0" to "#2A323E",   // 分割线
        "#EEEEEE" to "#2A323E",   // 分割线
    )

    fun apply(root: View, dark: Boolean) {
        walk(root, dark)
    }

    private fun walk(view: View, dark: Boolean) {
        // 递归子 View
        if (view is ViewGroup) {
            for (child in view.children) {
                walk(child, dark)
            }
        }

        // 处理背景色
        try {
            val bg = view.background
            if (bg is android.graphics.drawable.ColorDrawable) {
                val c = bg.color
                val hex = String.format("#%06X", 0xFFFFFF and c)
                val mapped = if (dark) COLOR_MAP_LIGHT_TO_DARK[hex] else null
                if (mapped != null) {
                    view.setBackgroundColor(Color.parseColor(mapped))
                }
            }
        } catch (_: Throwable) {}

        // 处理文字颜色
        if (view is TextView) {
            try {
                val c = view.currentTextColor
                val hex = String.format("#%06X", 0xFFFFFF and c)
                val mapped = if (dark) COLOR_MAP_LIGHT_TO_DARK[hex] else null
                if (mapped != null) {
                    view.setTextColor(Color.parseColor(mapped))
                }
            } catch (_: Throwable) {}
        }

        // CardView 背景
        if (view is CardView) {
            try {
                val c = view.cardBackgroundColor.defaultColor
                val hex = String.format("#%06X", 0xFFFFFF and c)
                val mapped = if (dark) COLOR_MAP_LIGHT_TO_DARK[hex] else null
                if (mapped != null) {
                    view.setCardBackgroundColor(Color.parseColor(mapped))
                }
            } catch (_: Throwable) {}
        }
    }
}
