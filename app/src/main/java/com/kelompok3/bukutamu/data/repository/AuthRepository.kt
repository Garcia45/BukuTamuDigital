package com.kelompok3.bukutamu.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.kelompok3.bukutamu.data.dao.UserDao
import com.kelompok3.bukutamu.data.entity.User
import com.kelompok3.bukutamu.util.PasswordHasher

/**
 * Repository untuk operasi autentikasi (Register & Login) ke UserDao.
 * Menjamin username di-trim dan password di-hash dengan SHA-256 + salt sebelum disimpan.
 */
class AuthRepository(private val userDao: UserDao) {

    /**
     * Mencari user berdasarkan username yang sudah di-trim.
     */
    suspend fun findByUsername(username: String): User? {
        return userDao.findByUsername(username.trim())
    }

    /**
     * Mendaftarkan akun baru (FR-01, FR-02).
     * Password di-hash menggunakan PasswordHasher sebelum disimpan ke database.
     *
     * @return ID user baru jika berhasil, atau null jika username sudah dipakai.
     */
    suspend fun registerUser(username: String, rawPassword: String): Long? {
        val cleanUsername = username.trim()
        if (userDao.findByUsername(cleanUsername) != null) {
            return null
        }
        val hashedPassword = PasswordHasher.hashPassword(rawPassword)
        return try {
            userDao.insert(
                User(
                    username = cleanUsername,
                    password = hashedPassword
                )
            )
        } catch (e: SQLiteConstraintException) {
            null
        }
    }

    /**
     * Memverifikasi kredensial login (FR-04, FR-05).
     *
     * @return objek User jika username ditemukan dan hash password cocok, atau null jika salah.
     */
    suspend fun authenticate(username: String, rawPassword: String): User? {
        val cleanUsername = username.trim()
        val user = userDao.findByUsername(cleanUsername) ?: return null
        val isPasswordValid = PasswordHasher.verifyPassword(rawPassword, user.password)
        return if (isPasswordValid) user else null
    }
}
