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
        // 背景
        "#EEF4FF" to "#0F1620",   // 页面背景
        "#FFFFFF" to "#1C2430",   // 卡片背景
        "#F5F7FB" to "#161E2A",   // 次级卡片
        "#F5F9FF" to "#161E2A",   // 极浅蓝
        "#F0F4FA" to "#161E2A",   // 聊天背景
        "#EAF0FF" to "#1E2B4D",   // 主色柔和底
        "#E0E9FF" to "#2A323E",   // 进度条底

        // 主色
        "#2E5BFF" to "#4A6BFF",   // 主蓝
        "#4A6BFF" to "#5E7CFF",   // 主蓝浅
        "#2E4A8E" to "#1E2B4D",   // 渐变起色
        "#1677FF" to "#4A6BFF",   // 支付宝蓝

        // 文字
        "#0F1A3C" to "#E8EAED",   // 主文字
        "#1F2937" to "#E8EAED",   // 正文/图标
        "#333333" to "#D0D0D0",   // 正文
        "#475569" to "#94A3B8",   // 中灰
        "#64748B" to "#98A2B3",   // 次要文字
        "#888888" to "#7A8494",   // 提示灰
        "#94A3B8" to "#7A8494",   // 提示文字
        "#98A2B3" to "#7A8494",   // 提示文字
        "#6b6d6a" to "#7A8494",   // 超时灰
        "#9CA3AF" to "#6B7280",   // 输入提示

        // 状态
        "#E53935" to "#EF4444",   // 危险
        "#ce665e" to "#EF4444",   // 超限
        "#FFE0E0" to "#4A1F1F",   // 危险柔和
        "#D97706" to "#F59E0B",   // 警告
        "#fab610" to "#F59E0B",   // 橙色
        "#0A9F52" to "#10B981",   // 成功
        "#2a9843" to "#34D399",   // 成功绿
        "#2b9a45" to "#34D399",   // 成功绿
        "#66b367" to "#34D399",   // 浅绿
        "#2E7D32" to "#10B981",   // 深绿
        "#26A17B" to "#10B981",   // Tether 绿

        // 分割线 / 边框
        "#F0F0F0" to "#2A323E",
        "#EEEEEE" to "#2A323E",
        "#E2E8F0" to "#2A323E",

        // 禁用
        "#D1D5D9" to "#3A4250",

        // 半透明绿（页面里的透明度色）
        "#5066b367" to "#2034D399",
        "#2066b367" to "#1034D399",
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
