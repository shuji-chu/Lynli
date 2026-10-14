package com.github.kr328.clash

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.network.ApiClient
import com.github.kr328.clash.design.network.ApiService
import com.github.kr328.clash.design.network.safeApiRequestCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AccountActivity : AppCompatActivity() {

    private val PICK_AVATAR = 1001
    private lateinit var ivAvatar: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        ivAvatar = findViewById(R.id.iv_avatar)
        tvNickname = findViewById(R.id.tv_nickname_value)
        tvEmail = findViewById(R.id.tv_email)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        // 头像点击
        findViewById<View>(R.id.frame_avatar).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_AVATAR)
        }

        // 昵称点击
        findViewById<View>(R.id.item_nickname).setOnClickListener { showEditNicknameDialog() }

        // 修改密码
        findViewById<View>(R.id.item_change_password).setOnClickListener { showChangePasswordDialog() }

        loadUserInfo()
    }

    private fun loadUserInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val resp = safeApiRequestCall { api.getUserData(auth) }
                val data = resp?.body()?.data
                withContext(Dispatchers.Main) {
                    tvEmail.text = data?.email ?: PreferenceManager.loginemail
                    tvNickname.text = data?.nickname ?: ""
                    // 头像
                    val av = data?.avatarURL ?: ""
                    if (av.startsWith("data:image")) {
                        try {
                            val base64 = av.substringAfter(",")
                            val bytes = Base64.decode(base64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ivAvatar.setImageBitmap(bmp)
                            ivAvatar.setColorFilter(null)
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun showEditNicknameDialog() {
        val input = EditText(this).apply {
            hint = "请输入新昵称"
            setText(tvNickname.text)
            setPadding(50, 30, 50, 30)
        }
        AlertDialog.Builder(this)
            .setTitle("修改昵称")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val nick = input.text.toString().trim()
                if (nick.isEmpty()) {
                    Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveNickname(nick)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveNickname(nick: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val resp = safeApiRequestCall { api.updateNickname(auth, mapOf("nickname" to nick)) }
                withContext(Dispatchers.Main) {
                    if (resp?.isSuccessful == true && resp.body()?.status == "success") {
                        tvNickname.text = nick
                        Toast.makeText(this@AccountActivity, "昵称已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AccountActivity, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountActivity, "网络错误：" + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        val oldPw = EditText(this).apply {
            hint = "原密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newPw = EditText(this).apply {
            hint = "新密码（至少6位）"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(oldPw)
        layout.addView(newPw)

        AlertDialog.Builder(this)
            .setTitle("修改密码")
            .setView(layout)
            .setPositiveButton("确认修改") { _, _ ->
                val o = oldPw.text.toString()
                val n = newPw.text.toString()
                if (o.isEmpty() || n.length < 6) {
                    Toast.makeText(this, "请填写完整，新密码至少6位", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                savePassword(o, n)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun savePassword(oldPw: String, newPw: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val resp = safeApiRequestCall {
                    api.updatePassword(auth, mapOf("old_password" to oldPw, "new_password" to newPw))
                }
                withContext(Dispatchers.Main) {
                    val msg = resp?.body()?.message ?: ""
                    if (resp?.isSuccessful == true && resp.body()?.status == "success") {
                        Toast.makeText(this@AccountActivity, "密码已修改", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AccountActivity, if (msg.isNotEmpty()) msg else "修改失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountActivity, "网络错误：" + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            try {
                val input = contentResolver.openInputStream(uri) ?: return
                val bmp = BitmapFactory.decodeStream(input)
                input.close()
                // 缩放
                val maxSize = 256
                val scale = kotlin.math.min(maxSize.toFloat() / bmp.width, maxSize.toFloat() / bmp.height)
                val finalBmp = if (scale < 1) Bitmap.createScaledBitmap(
                    bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
                ) else bmp
                // 转 base64
                val baos = ByteArrayOutputStream()
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val b64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                ivAvatar.setImageBitmap(finalBmp)
                ivAvatar.setColorFilter(null)
                uploadAvatar(b64)
            } catch (e: Throwable) {
                Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadAvatar(b64: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val auth = PreferenceManager.loginauthData ?: ""
                val resp = safeApiRequestCall { api.updateAvatar(auth, mapOf("avatar" to b64)) }
                withContext(Dispatchers.Main) {
                    if (resp?.isSuccessful == true && resp.body()?.status == "success") {
                        Toast.makeText(this@AccountActivity, "头像已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AccountActivity, "头像上传失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
