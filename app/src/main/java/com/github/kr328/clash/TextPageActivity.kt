package com.github.kr328.clash

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TextPageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_page)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "文本"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""

        findViewById<TextView>(R.id.tv_page_title).text = title
        findViewById<TextView>(R.id.tv_page_content).text = content
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
    }
}
