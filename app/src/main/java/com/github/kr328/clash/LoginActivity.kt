package com.github.kr328.clash

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.common.compat.isAllowForceDarkCompat
import com.github.kr328.clash.common.compat.isLightNavigationBarCompat
import com.github.kr328.clash.common.compat.isLightStatusBarsCompat
import com.github.kr328.clash.common.compat.isSystemBarsTranslucentCompat
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.PreferenceManager
import com.github.kr328.clash.design.databinding.ActivityLoginBinding
import com.github.kr328.clash.design.databinding.ActivityProfileBinding
import com.github.kr328.clash.design.ui.DayNight
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedBoolean
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.network.ApiClient
import com.github.kr328.clash.network.ApiClientConfig
import com.github.kr328.clash.network.ApiService
import com.github.kr328.clash.network.ConfigResponse
import com.github.kr328.clash.network.LoginRequest
import com.github.kr328.clash.network.LoginResponse
import com.github.kr328.clash.network.safeApiCall
import com.github.kr328.clash.network.safeApiRequestCall
import com.github.kr328.clash.utity.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService

    private var dayNight: DayNight = DayNight.Day
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 禁用返回键
        // 不调用 super.onBackPressed()
    }


    private var isPasswordVisible = false
    private var configRetryCount = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        applyDayNight()
        super.onCreate(savedInstanceState)
        applySystemBars()

          val binding = ActivityLoginBinding
            .inflate(this.layoutInflater, this.root, false)

        setContentView(binding.root)
        if (com.github.kr328.clash.design.PreferenceManager.darkMode == "dark") { ThemeHelper.apply(window.decorView, true) }

        PreferenceManager.init(this)

        binding.togglePasswordVisibility.setOnClickListener {

            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // 显示明文密码
                binding.loginPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.togglePasswordVisibility.setImageResource(R.drawable.visibility_24px) // 更改图标为显示状态
            } else {
                // 显示星号密码
                binding.loginPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.togglePasswordVisibility.setImageResource(R.drawable.visibility_off_24px) // 更改图标为隐藏状态
            }
            // 将光标移动到文本的末尾
            binding.loginPasswordEditText.setSelection(binding.loginPasswordEditText.text.length)

        }
        binding.loginLoginButton.setOnClickListener {
            val email = binding.loginEmailEditText.text.toString()
            val password = binding.loginPasswordEditText.text.toString()
            val isAgreementChecked = binding.loginAgreementCheckBox.isChecked


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
            } else if (!isAgreementChecked) {
                Toast.makeText(this, "请同意隐私政策和用户协议", Toast.LENGTH_SHORT).show()
            } else {

                // Hide the keyboard
                try {
                    val __d = java.io.File(getExternalFilesDir(null), "crash"); __d.mkdirs()
                    java.io.File(__d, "login.txt").appendText("${java.util.Date()} 点登录 email=" + email + "\n")
                } catch (_: Throwable) {}
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocusView = currentFocus
                if (currentFocusView != null) {
                    inputMethodManager.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
                performLogin(email, password)
            }
        }

        binding.loginForgotPassword.setOnClickListener {
            // Navigate to Forgot Password screen or show dialog
        }

        binding.loginPrivacy.setOnClickListener { showAgreementDialog() }

        binding.loginRegister.setOnClickListener {
            // Navigate to Register screen
            //startActivity(RegisterActivity::class::intent)
           // startActivity(RegisterActivity::class::intent)
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
           // finish()  // It

        }


        // ============ GitHub OAuth via WebView（不改 Manifest、不跳外部浏览器）============
        binding.btnGithubLogin.setOnClickListener { openGithubLogin() }

        apiService = ApiClientConfig.retrofit.create(ApiService::class.java)

        //获取 Config 数据
        getconfig()

        // 打开 APP 就检查更新
        UpdateChecker.check(this)
    }

    private fun getconfig(){

        runOnUiThread {
            LoadingDialog.show(this, "正在初始化...")
        }
        CoroutineScope(Dispatchers.IO).launch {

            safeApiCall {apiService.getConfig()}.let { env ->
                if (env != null && env.code == 1) {
                    withContext(Dispatchers.Main) {
                        LoadingDialog.hide()
                    }
                    PreferenceManager.saveConfigToPreferences(env)
                }else{
                    withContext(Dispatchers.Main) {
                        LoadingDialog.hide()
                    }
                }
            }
        }
    }

    /**
     *  withContext(Dispatchers.Main) {
     *             if (response.isSuccessful) {
     *                 val userData = response.body()
     *                 // 更新 UI
     *             } else {
     *                 Toast.makeText(this@MainActivity, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
     *             }
     *         }
     * */

    private fun applyDayNight(config: Configuration = resources.configuration) {
        // 主题样式部分（必须在 super.onCreate 之前调用）
        val mode = com.github.kr328.clash.design.PreferenceManager.darkMode
        val dayNight = when (mode) {
            "dark" -> DayNight.Night
            "auto" -> if (config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) DayNight.Night else DayNight.Day
            else -> DayNight.Day
        }
        try {
            when (dayNight) {
                DayNight.Night -> theme.applyStyle(R.style.AppThemeDark, true)
                DayNight.Day -> theme.applyStyle(R.style.AppThemeLight, true)
            }
        } catch (_: Throwable) {}
        this.dayNight = dayNight
    }

    private fun applySystemBars() {
        window.isAllowForceDarkCompat = false
        window.isSystemBarsTranslucentCompat = true
        try {
            window.statusBarColor = resolveThemedColor(android.R.attr.statusBarColor)
            window.navigationBarColor = resolveThemedColor(android.R.attr.navigationBarColor)
        } catch (_: Throwable) {}
        if (Build.VERSION.SDK_INT >= 23) {
            try { window.isLightStatusBarsCompat = resolveThemedBoolean(android.R.attr.windowLightStatusBar) } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= 27) {
            try { window.isLightNavigationBarCompat = resolveThemedBoolean(android.R.attr.windowLightNavigationBar) } catch (_: Throwable) {}
        }
    }


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

    private fun showWebViewDialog(url: String) {
        val webView = android.webkit.WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl(url)

        android.app.AlertDialog.Builder(this)
            .setView(webView)
            .setPositiveButton("关闭") { _, _ -> webView.destroy() }
            .setOnDismissListener { webView.destroy() }
            .create()
            .show()
    }

    private fun openGithubLogin() {
        val dlg = android.app.Dialog(this)
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val title = android.widget.TextView(this).apply {
            text = "GitHub 授权"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F1A3C"))
            setPadding(40, 40, 40, 20)
        }
        root.addView(title)

        val wv = android.webkit.WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = settings.userAgentString + " LynliApp"
            webViewClient = object : android.webkit.WebViewClient() {
                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean {
                    if (url != null && url.contains("/passport/oauth/github/callback") && url.contains("code=")) {
                        val code = android.net.Uri.parse(url).getQueryParameter("code") ?: ""
                        if (code.isNotEmpty()) {
                            dlg.dismiss()
                            exchangeGithubCode(code)
                            return true
                        }
                    }
                    return false
                }
            }
        }
        root.addView(wv, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val cancel = android.widget.TextView(this).apply {
            text = "取消"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#64748B"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 30)
            setOnClickListener { dlg.dismiss() }
        }
        root.addView(cancel)

        dlg.setContentView(root)
        dlg.setOnShowListener {
            dlg.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt()
            )
        }
        dlg.show()
        wv.loadUrl("https://lynli.kz.ci/api/v1/passport/oauth/github/start")
    }

    private fun exchangeGithubCode(code: String) {
        LoadingDialog.show(this, "登录中...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val resp = safeApiRequestCall { api.oauthGithubExchange(mapOf("code" to code)) }
                withContext(Dispatchers.Main) { LoadingDialog.hide() }
                val body = resp?.body()
                val d = body?.data
                if (resp != null && resp.isSuccessful && body?.code == 1 && d?.token?.isNotEmpty() == true) {
                    PreferenceManager.loginauthData = d.token ?: ""
                    PreferenceManager.loginToken = d.token ?: ""
                    PreferenceManager.loginemail = d.email ?: ""
                    PreferenceManager.isLoginin = true
                    withContext(Dispatchers.Main) {
                        val i = Intent(this@LoginActivity, MainActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(i)
                        finish()
                    }
                } else {
                    val msg = body?.message ?: "登录失败"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    LoadingDialog.hide()
                    Toast.makeText(this@LoginActivity, "网络异常: " + (e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        try {
            val __d = java.io.File(getExternalFilesDir(null), "crash"); __d.mkdirs()
            java.io.File(__d, "login.txt").appendText("${java.util.Date()} 进入 performLogin\n")
        } catch (_: Throwable) {}
        // 虚拟登录：特定账号直接放行 / Demo login: bypass with special account
        if (email == "demo@lynli.com" && password == "123456") {
            PreferenceManager.loginemail = email
            PreferenceManager.loginToken = "demo-token"
            PreferenceManager.loginauthData = "demo-auth"
            PreferenceManager.isLoginin = true
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Show the loading indicator with a custom message
        LoadingDialog.show(this, "正在登录...")

        val apiServiceApp = ApiClient.retrofit.create(ApiService::class.java)

        //获取 Config 数据
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val __d = java.io.File(getExternalFilesDir(null), "crash"); __d.mkdirs()
                java.io.File(__d, "login.txt").appendText("${java.util.Date()} 准备发请求\n")
            } catch (_: Throwable) {}

            var _resp: retrofit2.Response<LoginResponse>? = null
            try {
                _resp = safeApiRequestCall {
                    apiServiceApp.loginUser(LoginRequest(email,password))
                }
            } catch (e: Throwable) {
                try {
                    val __d = java.io.File(getExternalFilesDir(null), "crash"); __d.mkdirs()
                    java.io.File(__d, "login.txt").appendText("${java.util.Date()} 登录异常: " + e.javaClass.name + ": " + e.message + "\n" + e.stackTraceToString() + "\n")
                } catch (_: Throwable) {}
            }
            try {
                val __d = java.io.File(getExternalFilesDir(null), "crash"); __d.mkdirs()
                java.io.File(__d, "login.txt").appendText("${java.util.Date()} 收到响应 code=" + (_resp?.code() ?: -1) + "\n")
            } catch (_: Throwable) {}
            _resp.let {
                withContext(Dispatchers.Main) {
                    LoadingDialog.hide()
                }
                if (it != null &&
                    it.isSuccessful) {
                    val response: LoginResponse? = it.body()
                    println(response )
                    println(it.errorBody()?.string()  ?: "")
                    if (response?.data != null){
                        PreferenceManager.loginemail = email
                        PreferenceManager.loginToken =  response.data?.token ?: ""
                        PreferenceManager.loginauthData =  response.data?.auth_data ?: ""
                        PreferenceManager.isLoginin = true
//                        saveLoginMailToken(this@LoginActivity,email)
//                        saveLoginToken(this@LoginActivity, response.data.token ?: "")
//                        saveLoginAuthData(this@LoginActivity, response.data.auth_data ?: "")
//                        saveLoginStatus(this@LoginActivity,true)
                        //获取用户信息

                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()  // It
                        }
                    }
                }else{
                    try {
                        val errorResponse = it?.errorBody()?.string() ?: ""
                        var message = ""
                        if (errorResponse.isNotEmpty()) {
                            try {
                                message = JSONObject(errorResponse).optString("message")
                            } catch (_: Throwable) {}
                        }
                        if (message.isNullOrEmpty()) message = "网络异常，请检查网络后重试"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                        }
                    } catch (_: Throwable) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "登录失败，请重试", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

        }


    }
}
