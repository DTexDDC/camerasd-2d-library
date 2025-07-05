package com.example.datdt.scanningapp2D

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.datdt.scanningapp2D.R
import com.example.datdt.scanningsdk2D.DetectionManager


class RetryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retry)

        findViewById<Button>(R.id.retryButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)  // Replace with your SDK launcher Activity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}