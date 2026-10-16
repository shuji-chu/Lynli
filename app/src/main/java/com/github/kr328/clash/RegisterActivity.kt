package com.github.kr328.clash

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.common.compat.isAllowForceDarkCompat
import com.github.kr328.clash.common.compat.isLightNavigationBarCompat
import com.github.kr328.clash.common.compat.isLightStatusBarsCompat
import com.github.kr328.clash.common.compat.isSystemBarsTranslucentCompat
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.databinding.ActivityRegisterBinding
import com.github.kr328.clash.design.ui.DayNight
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedBoolean
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.LoginResponse
import com.github.kr328.clash.network.RegisterRequest
import com.github.kr328.clash.network.SendCodeRequest
import com.github.kr328.clash.network.safeApiRequestCall
import com.github.kr328.clash.utity.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import com.github.kr328.clash.design.util.BitmapUtil

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var apiService: ApiService
    private var dayNight: DayNight = DayNight.Day
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding
            .inflate(this.layoutInflater, this.root, false)
        setContentView(binding.root)

        applyDayNight()
        PreferenceManager.init(this)

        apiService = ApiClient.retrofit.create(ApiService::class.java)

        setupInputWatchers()
        setupClickListeners()
        refreshSubmitButton()
    }

    // ===================== 输入监听 =====================
    private fun setupInputWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                refreshSendCodeButton()
                refreshSubmitButton()
            }
        }
        binding.loginEmailEditText.addTextChangedListener(watcher)
        binding.loginPasswordEditText.addTextChangedListener(watcher)
        binding.loginPasswordEditText2.addTextChangedListener(watcher)
        binding.loginCodeEditText.addTextChangedListener(watcher)

        binding.loginAgreementCheckBox.setOnCheckedChangeListener { _, _ -> refreshSubmitButton() }

        // 邮箱失焦校验
        binding.loginEmailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = binding.loginEmailEditText.text.toString().trim()
                if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.loginEmailEditText.error = "邮箱格式不正确"
                }
            }
        }

        // 密码失焦校验
        binding.loginPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val pwd = binding.loginPasswordEditText.text.toString()
                if (pwd.isNotEmpty() && pwd.length < 6) {
                    binding.loginPasswordEditText.error = "密码至少 6 位"
                }
            }
        }

        // 确认密码失焦校验
        binding.loginPasswordEditText2.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val p1 = binding.loginPasswordEditText.text.toString()
                val p2 = binding.loginPasswordEditText2.text.toString()
                if (p2.isNotEmpty() && p1 != p2) {
                    binding.loginPasswordEditText2.error = "两次密码不一致"
                }
            }
        }
    }

    private fun isEmailValid(): Boolean {
        val email = binding.loginEmailEditText.text.toString().trim()
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isCodeReady(): Boolean {
        return binding.loginCodeEditText.text.toString().trim().length == 6
    }

    private fun isFormValid(): Boolean {
        val pwd = binding.loginPasswordEditText.text.toString()
        val pwd2 = binding.loginPasswordEditText2.text.toString()
        return isEmailValid()
                && pwd.length >= 6
                && pwd == pwd2
                && binding.loginCodeEditText.text.toString().trim().isNotEmpty()
                && binding.loginAgreementCheckBox.isChecked
    }

    private fun refreshSendCodeButton() {
        // 倒计时进行中，不覆盖
        if (countDownTimer != null) return
        binding.loginSendCodeButton.isEnabled = isEmailValid()
    }

    private fun refreshSubmitButton() {
        binding.loginLoginButton.isEnabled = isFormValid()
    }

    // ===================== 按钮点击 =====================
    private fun setupClickListeners() {
        binding.loginSendCodeButton.setOnClickListener {
            if (!isEmailValid()) {
                binding.loginEmailEditText.error = "请先输入正确的邮箱"
                return@setOnClickListener
            }
            showCaptcha()
        }

        binding.loginLoginButton.setOnClickListener {
            if (!isFormValid()) return@setOnClickListener
            hideKeyboard()
            doRegister()
        }

        binding.loginRegister.setOnClickListener {
            finish()
        }

        binding.loginForgotPassword.setOnClickListener {
            // 预留
        }

        binding.loginPrivacy.setOnClickListener { showAgreementDialog() }
    }

    // ===================== 图形验证码 =====================
    private fun showCaptcha() {
        CoroutineScope(Dispatchers.IO).launch {
            val api = com.github.kr328.clash.network.ApiClient.retrofit.create(com.github.kr328.clash.network.ApiService::class.java)
            val res = com.github.kr328.clash.network.safeApiRequestCall { api.getCaptcha() }
            val d = res?.body()?.data
            withContext(Dispatchers.Main) {
                val img = d?.image
                if (d == null || img.isNullOrEmpty()) {
                    Toast.makeText(this@RegisterActivity, "验证码加载失败", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                showCaptchaDialog(d.id ?: "", img, d.hint ?: "请点击提示字符", d.width ?: 320, d.height ?: 200)
            }
        }
    }

    private fun showCaptchaDialog(cid: String, imgDataUrl: String, hint: String, w: Int, h: Int) {
        val b64 = imgDataUrl.substringAfter(",")
        val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        val bmp = BitmapUtil.decodeSampled(bytes, 256, 256)
        if (bmp == null) {
            Toast.makeText(this, "验证码图片解析失败", Toast.LENGTH_SHORT).show()
            return
        }

        val dlg = android.app.Dialog(this)
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        box.addView(android.widget.TextView(this).apply {
            text = hint
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F1A3C"))
            gravity = android.view.Gravity.CENTER
        })
        box.addView(android.widget.TextView(this).apply {
            text = "点错会换一张新的"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 12)
        })

        val img = android.widget.ImageView(this)
        img.setImageBitmap(bmp)
        val lp = android.widget.LinearLayout.LayoutParams(w, h)
        img.layoutParams = lp
        img.scaleType = android.widget.ImageView.ScaleType.FIT_XY

        img.setOnClickListener { v ->
            val touchX = lastTouchX
            val touchY = lastTouchY
            val realX = touchX / v.width.toFloat() * w
            val realY = touchY / v.height.toFloat() * h
            CoroutineScope(Dispatchers.IO).launch {
                val api = com.github.kr328.clash.network.ApiClient.retrofit.create(com.github.kr328.clash.network.ApiService::class.java)
                val body = mapOf<String, Any>("id" to cid, "x" to realX, "y" to realY)
                val r = com.github.kr328.clash.network.safeApiRequestCall { api.checkCaptcha(body) }
                withContext(Dispatchers.Main) {
                    if (r?.body()?.code == 1 && r.body()?.data?.pass == true) {
                        dlg.dismiss()
                        sendCode()
                    } else {
                        Toast.makeText(this@RegisterActivity, r?.body()?.message ?: "点错了", Toast.LENGTH_SHORT).show()
                        dlg.dismiss()
                        showCaptcha()
                    }
                }
            }
        }

        img.setOnTouchListener { v, ev ->
            if (ev.action == android.view.MotionEvent.ACTION_DOWN) {
                lastTouchX = ev.x
                lastTouchY = ev.y
            }
            false
        }
        img.isClickable = true

        box.addView(img)

        val refresh = android.widget.TextView(this).apply {
            text = "换一张"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#2E5BFF"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 0)
            setOnClickListener {
                dlg.dismiss()
                showCaptcha()
            }
        }
        box.addView(refresh)

        dlg.setContentView(box)
        dlg.show()
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // ===================== 发验证码 =====================
    private fun sendCode() {
      try {
        val email = binding.loginEmailEditText.text.toString().trim()
        binding.loginSendCodeButton.isEnabled = false
        binding.loginSendCodeButton.text = "发送中..."

        CoroutineScope(Dispatchers.IO).launch {
            val res = safeApiRequestCall { apiService.sendCode(SendCodeRequest(email)) }
            withContext(Dispatchers.Main) {
                if (res != null && res.isSuccessful) {
                    val body = res.body()
                    val msg = body?.data?.msg ?: "验证码已发送"
                    Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
                    startCountdown()
                } else {
                    binding.loginSendCodeButton.text = "获取验证码"
                    binding.loginSendCodeButton.isEnabled = true
                    showBackendError(res?.errorBody()?.string(), "发送失败")
                }
            }
        }
      } catch (e: Throwable) {
        try { binding.loginSendCodeButton.isEnabled = true; binding.loginSendCodeButton.text = "获取验证码" } catch (_: Throwable) {}
        Toast.makeText(this@RegisterActivity, "发送失败：" + (e.message ?: ""), Toast.LENGTH_SHORT).show()
      }
    }
    private fun startCountdown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60_000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                binding.loginSendCodeButton.text = "${sec}s 后重发"
                binding.loginSendCodeButton.isEnabled = false
            }

            override fun onFinish() {
                binding.loginSendCodeButton.text = "获取验证码"
                binding.loginSendCodeButton.isEnabled = isEmailValid()
                countDownTimer = null
            }
        }.start()
    }

    // ===================== 注册 =====================
    private fun doRegister() {
        val email = binding.loginEmailEditText.text.toString().trim()
        val password = binding.loginPasswordEditText.text.toString()
        val code = binding.loginCodeEditText.text.toString().trim()
        val inviteCode = try {
            binding.loginInviteCodeEditText.text.toString().trim().uppercase()
        } catch (_: Throwable) { "" }

        LoadingDialog.show(this, "正在注册...")
        binding.loginLoginButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val res = safeApiRequestCall {
                apiService.registerUser(RegisterRequest(email, password, code, inviteCode))
            }
            withContext(Dispatchers.Main) {
                LoadingDialog.hide()
                binding.loginLoginButton.isEnabled = true

                if (res != null && res.isSuccessful) {
                    val response: LoginResponse? = res.body()
                    if (response?.data != null) {
                        PreferenceManager.loginemail = email
                        PreferenceManager.loginToken = response.data?.token ?: ""
                        PreferenceManager.loginauthData = response.data?.auth_data ?: ""
                        PreferenceManager.isLoginin = true

                        Toast.makeText(this@RegisterActivity, "注册成功", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "注册失败：返回数据为空", Toast.LENGTH_LONG).show()
                    }
                } else {
                    showBackendError(res?.errorBody()?.string(), "注册失败")
                }
            }
        }
    }

    private fun showBackendError(errorBody: String?, fallback: String) {
        val text = try {
            if (errorBody.isNullOrEmpty()) fallback
            else JSONObject(errorBody).optString("message").ifEmpty { fallback }
        } catch (e: JSONException) {
            fallback
        }
        Toast.makeText(this@RegisterActivity, text, Toast.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // ===================== 主题 =====================
    private fun showAgreementDialog() {
        val items = arrayOf("《用户协议》", "《隐私政策》")
        android.app.AlertDialog.Builder(this)
            .setTitle("协议")
            .setItems(items) { _, which ->
                if (which == 0) {
                    startActivity(android.content.Intent(this, TextPageActivity::class.java).apply {
                        putExtra(TextPageActivity.EXTRA_TITLE, "用户协议")
                        putExtra(TextPageActivity.EXTRA_CONTENT, LegalTexts.TERMS)
                    })
                } else {
                    startActivity(android.content.Intent(this, TextPageActivity::class.java).apply {
                        putExtra(TextPageActivity.EXTRA_TITLE, "隐私政策")
                        putExtra(TextPageActivity.EXTRA_CONTENT, LegalTexts.PRIVACY)
                    })
                }
            }
            .show()
    }

    private fun applyDayNight(config: Configuration = resources.configuration) {
        window.isAllowForceDarkCompat = false
        window.isSystemBarsTranslucentCompat = true
        window.statusBarColor = resolveThemedColor(android.R.attr.statusBarColor)
        window.navigationBarColor = resolveThemedColor(android.R.attr.navigationBarColor)

        if (Build.VERSION.SDK_INT >= 23) {
            window.isLightStatusBarsCompat = resolveThemedBoolean(android.R.attr.windowLightStatusBar)
        }
        if (Build.VERSION.SDK_INT >= 27) {
            window.isLightNavigationBarCompat = resolveThemedBoolean(android.R.attr.windowLightNavigationBar)
        }
        this.dayNight = DayNight.Night
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
