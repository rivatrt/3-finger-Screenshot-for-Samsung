package com.example.threefingershot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaProjectionForegroundService : Service() {

    private val CHANNEL_ID = "MediaProjectionChannel"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    companion object {
        var lastIntentData: Intent? = null
        var lastResultCode: Int = 0
        var isProjectionReady = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ThreeFingerShot")
            .setContentText("Screen Capture Active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (code != 0 && data != null) {
            lastResultCode = code
            lastIntentData = data
            isProjectionReady = true
        }

        if (isProjectionReady && lastIntentData != null) {
            takeScreenshot(lastResultCode, lastIntentData!!)
        }

        return START_STICKY
    }

    fun takeScreenshot(resultCode: Int, data: Intent) {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val displayMetrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            val density = displayMetrics.densityDpi
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )

            var isCaptured = false

            imageReader?.setOnImageAvailableListener({ reader ->
                if (isCaptured) return@setOnImageAvailableListener

                val image: Image? = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes[0]
                    val buffer: ByteBuffer = planes.buffer
                    val pixelStride = planes.pixelStride
                    val rowStride = planes.rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmapWidth = width + rowPadding / pixelStride
                    val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(buffer)

                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                    isCaptured = true
                    saveScreenshot(croppedBitmap)

                    image.close()

                    // We keep the service alive for silent subsequent captures,
                    // but we release the virtual display to save memory.
                    virtualDisplay?.release()
                    imageReader?.close()
                }
            }, null)
        }, 500)
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "screenshot_$timestamp.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ThreeFingerShot/")
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ThreeFingerShot")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
        Log.d("ScreenshotService", "Screenshot saved: $filename")
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        isProjectionReady = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
