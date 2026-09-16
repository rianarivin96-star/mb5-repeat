package com.example.mb5autoclick

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_SHIZUKU = 100
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var logText: TextView

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshLog()
            handler.postDelayed(this, 500)
        }
    }

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

        val btnClearLog = Button(this)
        btnClearLog.text = "Bersihkan Log"
        btnClearLog.setOnClickListener {
            MacroAccessibilityService.eventLog.clear()
            refreshLog()
        }
        layout.addView(btnClearLog)

        val infoText = TextView(this)
        infoText.text = "\nLangkah:\n" +
                "1. Tap tombol 1 & 2 dulu, aktifkan service-nya\n" +
                "2. LIHAT LOG DI BAWAH INI — kalau service nyambung,\n" +
                "   harus muncul baris 'SERVICE CONNECTED'\n" +
                "3. Colok mouse, tekan tombol apapun — kalau kebaca,\n" +
                "   bakal muncul baris baru 'KeyEvent: keyCode=...'\n" +
                "4. Kalau nggak ada baris baru muncul SAMA SEKALI\n" +
                "   pas tombol ditekan, berarti event-nya beneran\n" +
                "   nggak nyampe ke service ini\n"
        layout.addView(infoText)

        val logLabel = TextView(this)
        logLabel.text = "=== LOG EVENT (auto-refresh) ==="
        logLabel.textSize = 14f
        layout.addView(logLabel)

        logText = TextView(this)
        logText.text = "(belum ada log)"
        logText.textSize = 12f
        logText.setPadding(0, 20, 0, 20)
        layout.addView(logText)

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun refreshLog() {
        val logs = MacroAccessibilityService.eventLog
        logText.text = if (logs.isEmpty()) {
            "(belum ada log — service belum connect atau belum ada key event)"
        } else {
            logs.joinToString("\n")
        }
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
