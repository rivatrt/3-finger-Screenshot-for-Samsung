package com.example.threefingershot

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.threefingershot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnableGesture.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnOpenAccessibilitySettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isServiceRunning = ThreeFingerAccessibilityService.isServiceRunning
        if (isServiceRunning) {
            binding.tvStatus.text = "Service Running"
            binding.tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.tvStatus.text = "Service Not Running"
            binding.tvStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }

    private fun checkAndRequestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    102
                )
            }
        }
    }
}
