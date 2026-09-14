package com.example.qucidemo

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.qucidemo.databinding.FloatingDictBinding

class DictAccessibilityService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private lateinit var binding: FloatingDictBinding
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var currentWord = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            binding = FloatingDictBinding.inflate(LayoutInflater.from(this))
            params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                gravity = Gravity.TOP or Gravity.END
                x = 0
                y = 160
                format = PixelFormat.TRANSLUCENT
            }
            wm.addView(binding.root, params)
            setupUI()
            applyFlags(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return
        val src = event.source ?: return
        val text = src.text?.toString() ?: return
        val s = src.textSelectionStart
        val e = src.textSelectionEnd
        if (s in 0..<e && e <= text.length) {
            val sel = text.substring(s, e).trim()
            if (sel.isNotBlank()) showWord(sel)
        }
    }

    private fun setupUI() {
        var downX = 0f
        var downY = 0f
        var downPX = 0
        var downPY = 0
        var moved = false
        binding.flButton.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    downPX = params.x
                    downPY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
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
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        else
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (::binding.isInitialized) wm.updateViewLayout(binding.root, params)
    }

    private fun showWord(word: String) {
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (::binding.isInitialized) wm.removeView(binding.root)
        super.onDestroy()
    }
}
