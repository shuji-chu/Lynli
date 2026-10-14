package com.github.kr328.clash

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class BusinessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business)
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
    }
}
