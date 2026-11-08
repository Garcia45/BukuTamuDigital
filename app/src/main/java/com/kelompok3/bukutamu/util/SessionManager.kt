package com.kelompok3.bukutamu.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Pengelola sesi pengguna menggunakan SharedPreferences sesuai PRD bagian 8.1 & 13.
 * Kunci yang disimpan: is_logged_in (Boolean), user_id (Int), username (String).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Menyimpan sesi pengguna setelah login berhasil (FR-06).
     */
    fun saveSession(userId: Int, username: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username.trim())
            .apply()
    }

    /**
     * Memeriksa apakah terdapat sesi pengguna yang aktif dan valid (FR-07).
     */
    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getInt(KEY_USER_ID, INVALID_USER_ID)
        return loggedIn && userId != INVALID_USER_ID
    }

    /**
     * Mengambil user_id dari sesi aktif. Mengembalikan INVALID_USER_ID (-1) jika belum login.
     */
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, INVALID_USER_ID)
    }

    /**
     * Mengambil username dari sesi aktif.
     */
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    /**
     * Menghapus seluruh data sesi saat logout (FR-08).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREF_NAME = "buku_tamu_session_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val INVALID_USER_ID = -1
    }
}

