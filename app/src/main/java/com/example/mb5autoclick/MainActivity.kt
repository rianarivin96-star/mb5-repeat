package com.example.mb5autoclick

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_SHIZUKU = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 100, 40, 40)

        val statusText = TextView(this)
        statusText.text = "MB5 Auto Click Setup"
        statusText.textSize = 18f
        layout.addView(statusText)

        val btnShizuku = Button(this)
        btnShizuku.text = "1. Request Izin Shizuku"
        btnShizuku.setOnClickListener {
            requestShizukuPermission()
        }
        layout.addView(btnShizuku)

        val btnAccessibility = Button(this)
        btnAccessibility.text = "2. Buka Accessibility Settings"
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        layout.addView(btnAccessibility)

        val infoText = TextView(this)
        infoText.text = "\nLangkah:\n" +
                "1. Pastikan app Shizuku sudah jalan (status Running)\n" +
                "2. Tap tombol 1, izinkan popup yang muncul\n" +
                "3. Tap tombol 2, cari 'MB5 Auto Click' di list, aktifkan\n" +
                "4. Colok mouse, tekan MB5 untuk test"
        layout.addView(infoText)

        setContentView(layout)
    }

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11()) {
            return
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
    }
}
