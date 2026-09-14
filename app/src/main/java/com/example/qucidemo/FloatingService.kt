package com.example.qucidemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import com.example.qucidemo.databinding.FloatingDictBinding

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var binding: FloatingDictBinding
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var currentWord = ""

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(NOTIF_ID, buildNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        binding = FloatingDictBinding.inflate(LayoutInflater.from(this))
        params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 160
            format = PixelFormat.TRANSLUCENT
        }
        wm.addView(binding.root, params)
        setupUI()
        applyFlags(false)
        instance = this
    }

    private fun setupUI() {
        var downX = 0f
        var downY = 0f
        var downPX = 0
        var downPY = 0
        var moved = false
        binding.flButton.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    downPX = params.x
                    downPY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) moved = true
                    params.x = downPX - dx.toInt()
                    params.y = downPY + dy.toInt()
                    wm.updateViewLayout(binding.root, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleCard()
                    true
                }
                else -> false
            }
        }
        binding.btnClose.setOnClickListener {
            binding.cardPanel.visibility = android.view.View.GONE
            applyFlags(false)
        }
        binding.btnQuery.setOnClickListener {
            val w = binding.etWord.text.toString().trim()
            if (w.isNotEmpty()) showWord(w)
        }
    }

    private fun toggleCard() {
        val show = binding.cardPanel.visibility != android.view.View.VISIBLE
        binding.cardPanel.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        applyFlags(show)
        if (show) binding.etWord.requestFocus()
    }

    private fun applyFlags(focusableForIme: Boolean) {
        params.flags = if (focusableForIme)
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        else
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (::binding.isInitialized) wm.updateViewLayout(binding.root, params)
    }

    fun showWord(word: String) {
        handler.post {
            if (word == currentWord && binding.cardPanel.visibility == android.view.View.VISIBLE) return@post
            currentWord = word
            binding.cardPanel.visibility = android.view.View.VISIBLE
            applyFlags(true)
            binding.etWord.setText(word)
            queryWord(word)
        }
    }

    private fun queryWord(word: String) {
        binding.tvWord.text = word
        binding.tvPhone.text = "查询中…"
        binding.tvExplain.text = ""
        Thread {
            try {
                val (phone, explains) = DictApi.lookup(word)
                handler.post {
                    binding.tvPhone.text = phone
                    binding.tvExplain.text =
                        if (explains.isNotEmpty()) explains.joinToString("\n") else "未找到释义"
                }
            } catch (ex: Exception) {
                handler.post {
                    binding.tvPhone.text = ""
                    binding.tvExplain.text = "查询失败：${ex.message}"
                }
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "取词服务", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("取词服务运行中")
            .setContentText("选中文字即可查词")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::binding.isInitialized) wm.removeView(binding.root)
        instance = null
        super.onDestroy()
    }

    companion object {
        var instance: FloatingService? = null
        private const val NOTIF_ID = 1001
        private const val CHANNEL = "quci_channel"
    }
}
