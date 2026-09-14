package com.example.qucidemo

import android.content.Intent
import android.net.Uri
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

        binding.btnOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "悬浮窗权限已开启", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
                return@setOnClickListener
            }
            startService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "悬浮取词已启动，去任意 App 选中英文试试", Toast.LENGTH_SHORT).show()
        }

        binding.btnTest.setOnClickListener {
            val w = binding.etTest.text.toString().trim()
            if (w.isEmpty()) {
                Toast.makeText(this, "请输入单词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (FloatingService.instance == null) startService(Intent(this, FloatingService::class.java))
            FloatingService.instance?.showWord(w)
        }
    }
}
