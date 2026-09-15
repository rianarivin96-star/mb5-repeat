package com.example.mb5autoclick

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import rikka.shizuku.Shizuku
import java.io.DataOutputStream

/**
 * PENTING SEBELUM PAKAI:
 * MB4/MB5 pada kebanyakan mouse di Android akan muncul sebagai KeyEvent
 * dengan keycode KEYCODE_BACK (MB4) atau KEYCODE_FORWARD (MB5).
 * TAPI ini TIDAK PASTI SAMA di semua device/driver mouse.
 *
 * LANGKAH WAJIB SEBELUM CODING LEBIH LANJUT:
 * 1. Jalankan app ini dengan logging di bawah.
 * 2. Colokin mouse, pencet MB5, lihat Logcat (filter tag "MB5Service").
 * 3. Cek keyCode yang muncul. Kalau bukan KEYCODE_FORWARD, ganti
 *    nilai TARGET_KEYCODE di bawah sesuai yang muncul di log.
 */

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MB5Service"

        // Ganti ini kalau ternyata keycode MB5 di HP kamu beda
        private const val TARGET_KEYCODE = KeyEvent.KEYCODE_FORWARD

        // Interval antar klik repeat (ms). Makin kecil = makin cepat.
        private const val CLICK_INTERVAL_MS = 80L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRepeating = false
    private var lastX = 0f
    private var lastY = 0f

    // Simpan posisi cursor terakhir dari event hover/move mouse
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility event standar, tidak dipakai untuk logic utama di sini
    }

    override fun onInterrupt() {
        stopRepeatClick()
    }

    /**
     * Menangkap event tombol (termasuk MB4/MB5 yang biasanya masuk sebagai KeyEvent).
     * WAJIB set flag FLAG_REQUEST_FILTER_KEY_EVENTS di accessibility_service_config.xml
     * supaya method ini dipanggil.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d(TAG, "KeyEvent diterima: keyCode=${event.keyCode} action=${event.action}")

        if (event.keyCode == TARGET_KEYCODE) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isRepeating) {
                        isRepeating = true
                        startRepeatClick()
                    }
                    // return true = event ini "dimakan", tidak diteruskan sebagai
                    // fungsi asli (forward navigation)
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    isRepeating = false
                    stopRepeatClick()
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    /**
     * Menangkap posisi cursor mouse secara realtime lewat generic motion event.
     * Perlu window overlay atau service tipe tertentu untuk bisa nerima ini secara
     * global — kalau tidak masuk, alternatifnya pakai posisi tap terakhir yang
     * di-track lewat cara lain (misal broadcast dari overlay service kecil).
     */
    fun updateCursorPosition(x: Float, y: Float) {
        lastX = x
        lastY = y
    }

    private fun startRepeatClick() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isRepeating) return
                performClickViaShizuku(lastX, lastY)
                handler.postDelayed(this, CLICK_INTERVAL_MS)
            }
        })
    }

    private fun stopRepeatClick() {
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Inject tap pakai shell command lewat Shizuku.
     * Butuh Shizuku sudah running & permission sudah di-grant sebelum ini dipanggil.
     */
    private fun performClickViaShizuku(x: Float, y: Float) {
        try {
            val cmd = "input tap ${x.toInt()} ${y.toInt()}"
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.flush()
            outputStream.close()
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal inject klik via Shizuku", e)
        }
    }
}
