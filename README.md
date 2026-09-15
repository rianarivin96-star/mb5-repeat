# MB5 Auto-Click Repeat — Setup Guide

## Konsep
- **MB5 ditekan & ditahan** → left click repeat otomatis di posisi cursor terakhir, sampai MB5 dilepas.
- **Left click biasa (tanpa MB5)** → jalan normal, tidak diganggu.

## Langkah Setup

### 1. Tambah dependency Shizuku
Di `build.gradle` (app level):
```gradle
dependencies {
    implementation 'dev.rikka.shizuku:api:13.1.5'
    implementation 'dev.rikka.shizuku:provider:13.1.5'
}
```

### 2. Deklarasi di AndroidManifest.xml
```xml
<service
    android:name=".MacroAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### 3. Request izin Shizuku di MainActivity
Sebelum service bisa inject klik, user harus buka app Shizuku, pastikan statusnya
"Running", lalu app kamu request permission ke Shizuku (lewat `Shizuku.requestPermission()`).

### 4. WAJIB DICEK DULU — keycode MB5 di HP kamu
Kode di `MacroAccessibilityService.kt` nebak MB5 = `KEYCODE_FORWARD`, tapi ini bisa beda
tergantung driver mouse & Android version. Cara verifikasi:
1. Install & jalankan app ini apa adanya (dengan `Log.d` yang sudah ada).
2. Nyalakan Accessibility Service-nya di Settings > Accessibility.
3. Colok mouse, tekan MB5, buka Logcat filter tag `MB5Service`.
4. Lihat `keyCode` yang muncul di log — kalau bukan `KEYCODE_FORWARD`, ganti
   `TARGET_KEYCODE` di kode sesuai yang muncul.

### 5. Soal posisi cursor (lastX, lastY)
Kode `updateCursorPosition()` perlu dipanggil dari luar (misal overlay service kecil
yang nangkep `onGenericMotionEvent` mouse hover) — AccessibilityService sendiri tidak
otomatis dapat event ini secara global. Kalau belum sempat bikin overlay-nya, sementara
bisa hardcode posisi tap dulu untuk testing.

## Kenapa harus lewat Shizuku?
Fungsi `input tap x y` butuh permission `INJECT_EVENTS` yang biasanya cuma dimiliki
system app. Shizuku memberi app biasa akses shell-level (`adb shell`) sehingga command
ini bisa jalan tanpa root penuh.

## Catatan
- Ini kerangka awal, bukan app siap pakai — masih perlu MainActivity untuk request
  Shizuku permission, dan overlay kecil untuk tracking posisi cursor real-time.
- Kalau HP kamu OEM tertentu (Xiaomi/Oppo/Vivo) mungkin perlu izin tambahan di
  battery optimization / autostart supaya service tidak dimatikan sistem.
