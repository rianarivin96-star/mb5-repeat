package com.example.mb5autoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

        // MB4 terbukti kirim KEYCODE_BACK (dari tes Chrome & Key Mapper)
        private const val TARGET_KEYCODE = KeyEvent.KEYCODE_BACK

        // Interval antar klik repeat (ms). Makin kecil = makin cepat.
        private const val CLICK_INTERVAL_MS = 80L

        // LOG YANG BISA DIBACA LANGSUNG DARI MainActivity — bukti konkret,
        // nggak perlu app logcat/toast/vibrate lagi.
        val eventLog = mutableListOf<String>()

        @Synchronized
        fun addLog(message: String) {
            val time = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            eventLog.add(0, "[$time] $message")
            if (eventLog.size > 50) eventLog.removeAt(eventLog.size - 1)
        }
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
     * Beberapa Android/OEM (MIUI, dll) butuh flag FLAG_REQUEST_FILTER_KEY_EVENTS
     * di-set MANUAL lewat kode juga, nggak cukup cuma dari file XML config.
     * Ini fallback biar lebih pasti kepasang.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        Log.d(TAG, "onServiceConnected — flag key filtering di-set manual")
        addLog("SERVICE CONNECTED — service berhasil nyambung")
        vibrate() // getar sekali pas service aktif, biar tau service-nya beneran connect
    }

    /**
     * Menangkap event tombol (termasuk MB4/MB5 yang biasanya masuk sebagai KeyEvent).
     * WAJIB set flag FLAG_REQUEST_FILTER_KEY_EVENTS di accessibility_service_config.xml
     * supaya method ini dipanggil.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d(TAG, "KeyEvent diterima: keyCode=${event.keyCode} action=${event.action}")
        addLog("KeyEvent: keyCode=${event.keyCode} action=${event.action}")

        // GETAR HP — lebih pasti kerasa dibanding Toast (Toast bisa ke-block sistem)
        if (event.action == KeyEvent.ACTION_DOWN) {
            vibrate()
        }

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

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal vibrate", e)
        }
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
     *
     * CATATAN: Shizuku.newProcess() versi terbaru statusnya private (tidak bisa
     * dipanggil langsung), jadi dipanggil lewat reflection.
     */
    private fun performClickViaShizuku(x: Float, y: Float) {
        try {
            val cmd = "input tap ${x.toInt()} ${y.toInt()}"
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null,
                arrayOf("sh", "-c", cmd),
                null,
                null
            ) as Process

            val outputStream = DataOutputStream(process.outputStream)
            outputStream.flush()
            outputStream.close()
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal inject klik via Shizuku", e)
        }
    }
}
