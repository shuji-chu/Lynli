package com.github.kr328.clash

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FaqActivity : AppCompatActivity() {

    // 数据结构
    data class Faq(val category: String, val question: String, val answer: String)

    private val allFaqs = listOf(
        Faq("热门问题", "连接不上怎么办",
            "请按以下步骤排查：\n\n1. 检查手机网络连接是否正常\n2. 尝试切换至其他节点\n3. 完全退出应用后重新打开\n4. 若问题仍未解决，请联系在线客服"),
        Faq("热门问题", "怎么购买套餐",
            "打开应用 → 底部导航「套餐」 → 选择您需要的套餐 → 点击「购买订阅」完成支付。\n\n支付成功后，套餐将自动开通，无需额外操作。"),
        Faq("热门问题", "怎么申请退款",
            "如对服务不满意，可在购买后 7 天内联系我们申请退款。\n\n未使用的流量全额退款，已使用的部分按比例扣除。\n\n请通过「联系我们」中的客服渠道申请。"),
        Faq("连接问题", "连接后没有网络",
            "1. 确认节点是否正常（试试切换其它节点）\n2. 检查应用是否被系统限制后台\n3. 部分应用需要重启才能生效\n4. 若仍无法使用，请联系客服"),
        Faq("连接问题", "连接速度慢",
            "1. 尝试切换其它节点（不同节点速度不同）\n2. 避开网络高峰时段\n3. 检查本地网络是否正常\n4. 关闭其它占用带宽的应用"),
        Faq("连接问题", "支持哪些设备",
            "Lynli 支持 Android 手机、平板等设备。\n\niOS / Windows / Mac 版本正在开发中。"),
        Faq("连接问题", "最多几台设备同时使用",
            "每个账号同时支持 3 台设备在线。\n\n如超出限制，最早登录的设备将自动断开。"),
        Faq("订单问题", "支持哪些支付方式",
            "我们支持以下支付方式：\n\n· 支付宝\n· 微信支付\n· USDT（加密货币）\n\n支付完成后立即生效。"),
        Faq("订单问题", "支付成功未开通",
            "若支付成功但套餐未开通：\n\n1. 等待 1-2 分钟（系统同步需要时间）\n2. 下拉刷新「我的」页面\n3. 仍未开通请联系客服，并提供支付截图"),
        Faq("订单问题", "如何申请发票",
            "如需发票，请通过「联系我们」的客服渠道申请。\n\n请提供订单号、开票信息（公司名/税号）。"),
        Faq("账号问题", "忘记密码",
            "在登录页点击「忘记密码」，输入注册邮箱获取验证码，即可重置密码。\n\n若无法接收邮件，请联系客服协助处理。"),
        Faq("账号问题", "修改密码",
            "登录后进入「我的」→「设置」→「账户安全」→「修改密码」。\n\n建议使用 8 位以上的复杂密码。"),
        Faq("账号问题", "账号被封",
            "若您的账号因违规操作被封禁：\n\n1. 确认是否与他人共用账号\n2. 是否用于违法用途\n\n如需申诉，请通过客服渠道提交。"),
        Faq("邀请奖励", "邀请规则",
            "邀请好友注册成功后：\n\n· 好友获赠 3 天 5GB 流量畅享\n· 您获得 15% 佣金，永久绑定\n\n邀请关系一旦建立，永久有效。"),
        Faq("邀请奖励", "佣金怎么提现",
            "佣金满 100 元即可申请提现。\n\n请通过客服渠道提交提现申请，我们将在 3 个工作日内处理。")
    )

    private lateinit var container: LinearLayout
    private lateinit var searchBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        container = findViewById(R.id.ll_faq_container)
        searchBox = findViewById(R.id.et_search)

        render(allFaqs)

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val k = s?.toString()?.trim() ?: ""
                val filtered = if (k.isEmpty()) allFaqs
                else allFaqs.filter { it.question.contains(k) || it.answer.contains(k) }
                render(filtered)
            }
        })
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun render(list: List<Faq>) {
        container.removeAllViews()

        if (list.isEmpty()) {
            val empty = TextView(this).apply {
                text = "未找到相关问题"
                textSize = 13f
                setTextColor(0xFF94A3B8.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, dp(40))
            }
            container.addView(empty)
            return
        }

        // 按分类分组
        val grouped = list.groupBy { it.category }
        for ((category, items) in grouped) {
            // 分类标题
            val title = TextView(this).apply {
                text = category
                textSize = 13f
                setTextColor(0xFF94A3B8.toInt())
                setPadding(dp(24), dp(24), 0, dp(8))
            }
            container.addView(title)

            // 每个分类下的问题
            for (faq in items) {
                container.addView(buildFaqItem(faq))
            }
        }
    }

    private fun buildFaqItem(faq: Faq): View {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 行：问题 + 箭头
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
            isClickable = true
            isFocusable = true
        }

        val question = TextView(this).apply {
            text = faq.question
            textSize = 15f
            setTextColor(0xFF1F2937.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(question)

        val arrow = TextView(this).apply {
            text = "▾"
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
        }
        row.addView(arrow)

        wrapper.addView(row)

        // 答案区（默认隐藏）
        val answer = TextView(this).apply {
            text = faq.answer
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
            setPadding(dp(24), 0, dp(24), dp(16))
            visibility = View.GONE
            setLineSpacing(dp(6).toFloat(), 1f)
        }
        wrapper.addView(answer)

        // 分隔线
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
            setBackgroundColor(0xFFF0F0F0.toInt())
        }
        wrapper.addView(divider)

        // 点击展开
        var expanded = false
        row.setOnClickListener {
            expanded = !expanded
            answer.visibility = if (expanded) View.VISIBLE else View.GONE
            arrow.text = if (expanded) "▴" else "▾"
            question.setTypeface(null, if (expanded) Typeface.BOLD else Typeface.NORMAL)
        }

        return wrapper
    }
}
