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
        binding.login_loginButton.isEnabled = isFormValid()
    }

    // ===================== 按钮点击 =====================
    private fun setupClickListeners() {
        binding.loginSendCodeButton.setOnClickListener {
            if (!isEmailValid()) {
                binding.loginEmailEditText.error = "请先输入正确的邮箱"
                return@setOnClickListener
            }
            sendCode()
        }

        binding.login_loginButton.setOnClickListener {
            if (!isFormValid()) return@setOnClickListener
            hideKeyboard()
            doRegister()
        }

        binding.login_register.setOnClickListener {
            finish()
        }

        binding.loginForgotPassword.setOnClickListener {
            // 预留
        }
    }

    // ===================== 发验证码 =====================
    private fun sendCode() {
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

        LoadingDialog.show(this, "正在注册...")
        binding.login_loginButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val res = safeApiRequestCall {
                apiService.registerUser(RegisterRequest(email, password, code))
            }
            withContext(Dispatchers.Main) {
                LoadingDialog.hide()
                binding.login_loginButton.isEnabled = true

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
