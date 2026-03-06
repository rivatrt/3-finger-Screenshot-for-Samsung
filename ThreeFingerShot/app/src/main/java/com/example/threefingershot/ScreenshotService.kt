package com.example.threefingershot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ScreenshotService : Service() {

    private val CHANNEL_ID = "ScreenshotServiceChannel"

    private var windowManager: WindowManager? = null
    private var fullScreenOverlay: View? = null

    private var startY1 = 0f
    private var startY2 = 0f
    private var startY3 = 0f
    private var isTracking = false
    private val SWIPE_THRESHOLD = 150f

    // For pass-through recording
    private val recordedPath = Path()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        // Android 11+ uses native onGesture(30). For < 11 we need a full screen intercept-and-dispatch overlay.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            setupOverlay()
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        fullScreenOverlay = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                val pointerCount = event.pointerCount

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        recordedPath.reset()
                        recordedPath.moveTo(event.rawX, event.rawY)
                        isTracking = false
                        true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        if (pointerCount == 3) {
                            isTracking = true
                            startY1 = event.rawY
                            // Getting other pointer locations correctly
                            startY2 = event.getY(1) + (event.rawY - event.y)
                            startY3 = event.getY(2) + (event.rawY - event.y)
                        } else {
                            recordedPath.lineTo(event.rawX, event.rawY)
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isTracking && pointerCount == 3) {
                            val dy1 = event.rawY - startY1
                            val dy2 = (event.getY(1) + (event.rawY - event.y)) - startY2
                            val dy3 = (event.getY(2) + (event.rawY - event.y)) - startY3

                            if (dy1 > SWIPE_THRESHOLD && dy2 > SWIPE_THRESHOLD && dy3 > SWIPE_THRESHOLD) {
                                ThreeFingerAccessibilityService.instance?.triggerScreenshot()
                                isTracking = false // Reset
                                recordedPath.reset()
                            }
                        } else if (!isTracking) {
                            recordedPath.lineTo(event.rawX, event.rawY)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!isTracking && pointerCount < 3 && !recordedPath.isEmpty) {
                            recordedPath.lineTo(event.rawX, event.rawY)
                            // Replay non-3-finger gestures
                            ThreeFingerAccessibilityService.instance?.dispatchReplayedGesture(recordedPath)
                            recordedPath.reset()
                        }
                        if (pointerCount <= 3) {
                            isTracking = false
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        try {
            windowManager?.addView(fullScreenOverlay, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "ThreeFingerShot Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ThreeFingerShot")
            .setContentText("Service is running to detect gestures")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (fullScreenOverlay != null) {
            try {
                windowManager?.removeView(fullScreenOverlay)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
