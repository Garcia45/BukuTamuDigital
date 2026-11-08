package com.kelompok3.bukutamu.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utilitas hashing password menggunakan SHA-256 + salt acak sesuai PRD bagian 9.
 * Format penyimpanan di database: "<salt_hex>:<hash_hex>".
 */
object PasswordHasher {

    private const val SALT_LENGTH_BYTES = 16
    private const val DELIMITER = ":"
    private const val HASH_ALGORITHM = "SHA-256"

    /**
     * Membuat hash password baru dengan salt acak 16-byte.
     * Hasil berbentuk "saltHex:hashHex".
     */
    fun hashPassword(rawPassword: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        val saltHex = bytesToHex(salt)
        val hashHex = computeSha256Hex(saltHex, rawPassword)
        return "$saltHex$DELIMITER$hashHex"
    }

    /**
     * Memverifikasi apakah password teks biasa cocok dengan hash yang tersimpan di database.
     */
    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        val parts = storedHash.split(DELIMITER)
        if (parts.size != 2) return false
        val saltHex = parts[0]
        val expectedHashHex = parts[1]
        if (saltHex.isBlank() || expectedHashHex.isBlank()) return false

        val actualHashHex = computeSha256Hex(saltHex, rawPassword)
        return MessageDigest.isEqual(
            expectedHashHex.toByteArray(Charsets.UTF_8),
            actualHashHex.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Menghitung SHA-256(saltHex + rawPassword) dalam format heksadesimal.
     */
    fun computeSha256Hex(saltHex: String, rawPassword: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val combined = "$saltHex:$rawPassword".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(combined)
        return bytesToHex(hashBytes)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}

