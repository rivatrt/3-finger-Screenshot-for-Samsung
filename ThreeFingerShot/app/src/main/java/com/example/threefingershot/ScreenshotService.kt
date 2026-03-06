package com.example.threefingershot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class ScreenshotService : Service() {

    private val CHANNEL_ID = "ScreenshotServiceChannel"

    private var windowManager: WindowManager? = null
    private var floatingButtonView: View? = null

    // Draggable button variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isMoved = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        // Android 11+ uses native onGesture(30) with flagRequestMultiFingerGestures.
        // For < 11 we show a floating assistive button as a workaround since full-screen
        // touch interception without Touch Exploration causes UX-breaking infinite loops.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            setupFloatingButton()
        }
    }

    private fun setupFloatingButton() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val frameLayout = FrameLayout(this)

        val button = Button(this).apply {
            text = "\uD83D\uDCF7" // Camera Emoji
            setBackgroundColor(Color.parseColor("#AA000000"))
            setTextColor(Color.WHITE)
        }

        val buttonSize = (50 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(buttonSize, buttonSize)
        frameLayout.addView(button, params)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        button.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isMoved = true
                        layoutParams.x = initialX + deltaX.toInt()
                        layoutParams.y = initialY + deltaY.toInt()
                        windowManager?.updateViewLayout(frameLayout, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        ThreeFingerAccessibilityService.instance?.triggerScreenshot()
                    }
                    true
                }
                else -> false
            }
        }

        floatingButtonView = frameLayout

        try {
            windowManager?.addView(floatingButtonView, layoutParams)
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
        if (floatingButtonView != null) {
            try {
                windowManager?.removeView(floatingButtonView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
