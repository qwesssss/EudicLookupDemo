package com.example.qucidemo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class DictAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return
        val src = event.source ?: return
        val text = src.text?.toString() ?: return
        val s = src.textSelectionStart
        val e = src.textSelectionEnd
        if (s in 0..<e && e <= text.length) {
            val sel = text.substring(s, e).trim()
            if (sel.isNotBlank()) {
                ensureFloating()
                FloatingService.instance?.showWord(sel)
            }
        }
    }

    private fun ensureFloating() {
        if (!Settings.canDrawOverlays(this)) return
        if (FloatingService.instance == null) {
            startService(Intent(this, FloatingService::class.java))
        }
    }

    override fun onInterrupt() {}
}
