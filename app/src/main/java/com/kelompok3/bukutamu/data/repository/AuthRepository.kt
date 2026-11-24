package com.kelompok3.bukutamu.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.kelompok3.bukutamu.data.dao.UserDao
import com.kelompok3.bukutamu.data.entity.User
import com.kelompok3.bukutamu.data.remote.ApiResult
import com.kelompok3.bukutamu.data.remote.BukuTamuApiService
import com.kelompok3.bukutamu.util.PasswordHasher

/**
 * Repository untuk operasi autentikasi (Register & Login) (NIM 187).
 * Mendukung mode Backend PHP REST API (dengan cache sinkronisasi ke tabel Room users_187)
 * serta mode lokal murni saat apiService_187 bernilai null (misal pada unit test).
 */
class AuthRepository(
    private val userDao_187: UserDao,
    private val apiService_187: BukuTamuApiService? = null
) {

    sealed class RegisterOutcome {
        data class Success(val userId_187: Long) : RegisterOutcome()
        data class DuplicateUsername(val message_187: String) : RegisterOutcome()
        data class Error(val message_187: String) : RegisterOutcome()
    }

    sealed class LoginOutcome {
        data class Success(val user_187: User) : LoginOutcome()
        data class InvalidCredentials(val message_187: String) : LoginOutcome()
        data class Error(val message_187: String) : LoginOutcome()
    }

    suspend fun findByUsername(username_187: String): User? {
        return userDao_187.findByUsername(username_187.trim())
    }

    suspend fun registerUserWithOutcome(username_187: String, rawPassword_187: String): RegisterOutcome {
        val cleanUsername_187 = username_187.trim()
        if (apiService_187 != null) {
            return when (val result_187 = apiService_187.register(cleanUsername_187, rawPassword_187)) {
                is ApiResult.Success -> {
                    val cachedUser_187 = User(
                        id = result_187.data_187.id,
                        username = result_187.data_187.username,
                        password = PasswordHasher.hashPassword(rawPassword_187)
                    )
                    userDao_187.upsert(cachedUser_187)
                    RegisterOutcome.Success(result_187.data_187.id.toLong())
                }
                is ApiResult.HttpError -> {
                    if (result_187.code_187 == 409) {
                        RegisterOutcome.DuplicateUsername(result_187.message_187)
                    } else {
                        RegisterOutcome.Error(result_187.message_187)
                    }
                }
                is ApiResult.NetworkError -> {
                    RegisterOutcome.Error(result_187.message_187)
                }
            }
        }

        val insertedId_187 = registerUserLocal_187(cleanUsername_187, rawPassword_187)
        return if (insertedId_187 != null && insertedId_187 > 0L) {
            RegisterOutcome.Success(insertedId_187)
        } else {
            RegisterOutcome.DuplicateUsername(
                "Username sudah digunakan, silakan pilih username lain"
            )
        }
    }

    suspend fun registerUser(username_187: String, rawPassword_187: String): Long? {
        return when (val outcome_187 = registerUserWithOutcome(username_187, rawPassword_187)) {
            is RegisterOutcome.Success -> outcome_187.userId_187
            else -> null
        }
    }

    private suspend fun registerUserLocal_187(cleanUsername_187: String, rawPassword_187: String): Long? {
        if (userDao_187.findByUsername(cleanUsername_187) != null) {
            return null
        }
        val hashedPassword_187 = PasswordHasher.hashPassword(rawPassword_187)
        return try {
            userDao_187.insert(
                User(
                    username = cleanUsername_187,
                    password = hashedPassword_187
                )
            )
        } catch (e_187: SQLiteConstraintException) {
            null
        }
    }

    suspend fun authenticateWithOutcome(username_187: String, rawPassword_187: String): LoginOutcome {
        val cleanUsername_187 = username_187.trim()
        if (apiService_187 != null) {
            return when (val result_187 = apiService_187.login(cleanUsername_187, rawPassword_187)) {
                is ApiResult.Success -> {
                    val cachedUser_187 = User(
                        id = result_187.data_187.id,
                        username = result_187.data_187.username,
                        password = PasswordHasher.hashPassword(rawPassword_187)
                    )
                    userDao_187.upsert(cachedUser_187)
                    LoginOutcome.Success(result_187.data_187)
                }
                is ApiResult.HttpError -> {
                    if (result_187.code_187 == 401) {
                        LoginOutcome.InvalidCredentials(result_187.message_187)
                    } else {
                        LoginOutcome.Error(result_187.message_187)
                    }
                }
                is ApiResult.NetworkError -> {
                    LoginOutcome.Error(result_187.message_187)
                }
            }
        }

        val localUser_187 = authenticateLocal_187(cleanUsername_187, rawPassword_187)
        return if (localUser_187 != null) {
            LoginOutcome.Success(localUser_187)
        } else {
            LoginOutcome.InvalidCredentials("Username atau password salah")
        }
    }

    suspend fun authenticate(username_187: String, rawPassword_187: String): User? {
        return when (val outcome_187 = authenticateWithOutcome(username_187, rawPassword_187)) {
            is LoginOutcome.Success -> outcome_187.user_187
            else -> null
        }
    }

    private suspend fun authenticateLocal_187(cleanUsername_187: String, rawPassword_187: String): User? {
        val user_187 = userDao_187.findByUsername(cleanUsername_187) ?: return null
        val isPasswordValid_187 = PasswordHasher.verifyPassword(rawPassword_187, user_187.password)
        return if (isPasswordValid_187) user_187 else null
    }
}
