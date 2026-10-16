package com.github.kr328.clash.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

/**
 * 安全 Bitmap 解码：按目标尺寸采样，防止大图 OOM 崩溃
 */
object BitmapUtil {
    fun decodeSampled(bytes: ByteArray?, reqWidth: Int = 512, reqHeight: Int = 512): Bitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > reqWidth * 2 || bounds.outHeight / sample > reqHeight * 2) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) { null }
    }

    fun decodeSampledStream(input: InputStream?, reqWidth: Int = 512, reqHeight: Int = 512): Bitmap? {
        if (input == null) return null
        return try {
            decodeSampled(input.readBytes(), reqWidth, reqHeight)
        } catch (_: Throwable) { null }
    }
}
