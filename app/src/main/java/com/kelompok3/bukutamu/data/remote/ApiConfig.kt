package com.kelompok3.bukutamu.data.remote

/**
 * Konfigurasi alamat Backend PHP REST API (NIM 187).
 *
 * Panduan BASE_URL_187:
 * - Emulator Android (php -S 0.0.0.0:8000): "http://10.0.2.2:8000/"
 * - Emulator Android (Apache XAMPP htdocs/bukutamu-api): "http://10.0.2.2/bukutamu-api/"
 * - HP Fisik via USB Debugging (`adb reverse tcp:8000 tcp:8000`): "http://127.0.0.1:8000/"
 * - HP Fisik via Wi-Fi satu jaringan: "http://<IP_LAPTOP>:8000/"
 */
object ApiConfig {
    const val BASE_URL_187: String = "http://10.0.2.2:8000/"
    const val CONNECT_TIMEOUT_MS_187: Int = 8_000
    const val READ_TIMEOUT_MS_187: Int = 8_000
}
