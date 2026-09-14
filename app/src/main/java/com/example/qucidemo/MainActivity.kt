package com.example.qucidemo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qucidemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnTest.setOnClickListener {
            val w = binding.etTest.text.toString().trim()
            if (w.isEmpty()) {
                Toast.makeText(this, "请输入单词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Thread {
                try {
                    val (phone, explains) = DictApi.lookup(w)
                    val text = if (explains.isNotEmpty())
                        (if (phone.isNotEmpty()) "$phone\n" else "") + explains.joinToString("\n")
                    else "未找到释义"
                    runOnUiThread {
                        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
                    }
                } catch (ex: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "查询失败：${ex.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }
}
