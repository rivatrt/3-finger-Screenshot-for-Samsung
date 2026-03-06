package com.example.threefingershot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ThreeFingerAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceRunning = false
        var instance: ThreeFingerAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        instance = this
        Log.d("ThreeFingerShot", "Accessibility Service Connected")
        startScreenshotService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onGesture(gestureId: Int): Boolean {
        Log.d("ThreeFingerShot", "Gesture detected: $gestureId")
        if (gestureId == 30) {
            triggerScreenshot()
            return true
        }
        return super.onGesture(gestureId)
    }

    fun dispatchReplayedGesture(path: Path) {
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        instance = null
        Log.d("ThreeFingerShot", "Accessibility Service Disconnected")
        stopScreenshotService()
        return super.onUnbind(intent)
    }

    private fun startScreenshotService() {
        val intent = Intent(this, ScreenshotService::class.java)
        startService(intent)
    }

    private fun stopScreenshotService() {
        val intent = Intent(this, ScreenshotService::class.java)
        stopService(intent)
    }

    fun triggerScreenshot() {
        // If we already have the media projection token, use the foreground service directly
        if (MediaProjectionForegroundService.isProjectionReady && MediaProjectionForegroundService.lastIntentData != null) {
            val serviceIntent = Intent(this, MediaProjectionForegroundService::class.java).apply {
                putExtra("code", MediaProjectionForegroundService.lastResultCode)
                putExtra("data", MediaProjectionForegroundService.lastIntentData)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            // First time, need user consent
            val intent = Intent(this, ScreenshotActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }
}
