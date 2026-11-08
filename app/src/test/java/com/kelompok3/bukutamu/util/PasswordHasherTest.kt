package com.kelompok3.bukutamu.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk PasswordHasher sesuai PRD bagian 15.
 */
class PasswordHasherTest {

    @Test
    fun hashPassword_doesNotStorePlaintextAndHasExpectedFormat() {
        val raw = "rahasia123"
        val hashed = PasswordHasher.hashPassword(raw)

        assertNotEquals(raw, hashed)
        assertFalse(hashed.contains(raw))

        val parts = hashed.split(":")
        assertEquals(2, parts.size)
        // 16 bytes salt -> 32 hex chars; SHA-256 -> 64 hex chars
        assertEquals(32, parts[0].length)
        assertEquals(64, parts[1].length)
    }

    @Test
    fun hashPassword_generatesUniqueRandomSaltsForSamePassword() {
        val raw = "passwordSama123"
        val hash1 = PasswordHasher.hashPassword(raw)
        val hash2 = PasswordHasher.hashPassword(raw)

        assertNotEquals(hash1, hash2)
        assertTrue(PasswordHasher.verifyPassword(raw, hash1))
        assertTrue(PasswordHasher.verifyPassword(raw, hash2))
    }

    @Test
    fun verifyPassword_returnsTrueForCorrectPasswordAndFalseForWrongPassword() {
        val raw = "adminBukuTamu2026"
        val hashed = PasswordHasher.hashPassword(raw)

        assertTrue(PasswordHasher.verifyPassword(raw, hashed))
        assertFalse(PasswordHasher.verifyPassword("adminbukutamu2026", hashed))
        assertFalse(PasswordHasher.verifyPassword("salah123", hashed))
        assertFalse(PasswordHasher.verifyPassword("", hashed))
    }

    @Test
    fun verifyPassword_handlesMalformedStoredHashSafely() {
        assertFalse(PasswordHasher.verifyPassword("password123", "tanpa_delimiter"))
        assertFalse(PasswordHasher.verifyPassword("password123", ":"))
        assertFalse(PasswordHasher.verifyPassword("password123", "a:b:c"))
        assertFalse(PasswordHasher.verifyPassword("password123", ""))
    }

    @Test
    fun hashAndVerify_handlesLongAndSpecialCharacterPasswords() {
        val specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?~`😀🔒" + "x".repeat(1000)
        val hashed = PasswordHasher.hashPassword(specialPassword)

        assertTrue(PasswordHasher.verifyPassword(specialPassword, hashed))
        assertFalse(PasswordHasher.verifyPassword(specialPassword + "1", hashed))
    }
}

