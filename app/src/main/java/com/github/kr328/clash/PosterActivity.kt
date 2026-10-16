package com.github.kr328.clash

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.github.kr328.clash.design.PreferenceManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Hashtable
import com.github.kr328.clash.util.BitmapUtil

class PosterActivity : AppCompatActivity() {

    private var posterBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this)
        setContentView(R.layout.activity_poster)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        val iv = findViewById<ImageView>(R.id.iv_poster)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnShare = findViewById<Button>(R.id.btn_share)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        val inviteCode = intent.getStringExtra("invite_code") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bmp = generatePoster(inviteCode)
                posterBitmap = bmp
                withContext(Dispatchers.Main) {
                    iv.setImageBitmap(bmp)
                    tvStatus.text = "长按图片保存，或点下方按钮"
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) { tvStatus.text = "生成失败: " + e.message }
            }
        }

        btnSave.setOnClickListener {
            val b = posterBitmap ?: return@setOnClickListener
            CoroutineScope(Dispatchers.IO).launch {
                val ok = saveToGallery(b)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PosterActivity, if (ok) "已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnShare.setOnClickListener {
            val b = posterBitmap ?: return@setOnClickListener
            CoroutineScope(Dispatchers.IO).launch {
                val f = saveToCache(b)
                withContext(Dispatchers.Main) {
                    if (f != null) shareFile(f) else Toast.makeText(this@PosterActivity, "生成失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generatePoster(inviteCode: String): Bitmap {
        val bg = BitmapUtil.decodeSampledStream(assets.open("haibao.jpg"), 1080, 1920)
        val w = bg.width
        val h = bg.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(bg, 0f, 0f, null)

        val ver = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.4.87"
        } catch (_: Throwable) { "0.4.87" }
        val _cfg = com.github.kr328.clash.design.PreferenceManager.getConfigFromPreferences(this)
        val dlUrl = if (!_cfg?.appDownloadUrl.isNullOrEmpty())
            _cfg!!.appDownloadUrl!!
        else
            "https://github.com/shuji-chu/Lynli-Release/releases/latest/download/Lynli.apk"

        val qrSize = (w * 0.56f).toInt()
        val qr = makeQR(dlUrl, qrSize)
        val qrX = (w - qrSize) / 2f
        val qrY = h * 0.262f
        canvas.drawBitmap(qr, qrX, qrY, null)

        if (inviteCode.isNotEmpty()) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = w * 0.052f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                setShadowLayer(4f, 0f, 2f, Color.parseColor("#66000000"))
            }
            canvas.drawText("注册填邀请码：$inviteCode", w / 2f, h * 0.715f, paint)
        }

        return out
    }

    private fun makeQR(text: String, size: Int): Bitmap {
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private fun saveToGallery(b: Bitmap): Boolean {
        return try {
            val name = "lynli_poster_${System.currentTimeMillis()}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lynli")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                contentResolver.openOutputStream(uri)?.use { b.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                true
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Lynli")
                dir.mkdirs()
                val f = File(dir, name)
                FileOutputStream(f).use { b.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)))
                true
            }
        } catch (_: Throwable) { false }
    }

    private fun saveToCache(b: Bitmap): File? {
        return try {
            val f = File(cacheDir, "poster_share.jpg")
            FileOutputStream(f).use { b.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            f
        } catch (_: Throwable) { null }
    }

    private fun shareFile(f: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(i, "分享海报"))
        } catch (e: Throwable) {
            Toast.makeText(this, "分享失败: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }
}
