package com.kelompok3.bukutamu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kelompok3.bukutamu.data.entity.User
import com.kelompok3.bukutamu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk menangani logika autentikasi (Register & Login) sesuai FR-01 s.d. FR-08.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    sealed class LoginState {
        data object Idle : LoginState()
        data object Loading : LoginState()
        data class ValidationError(
            val usernameError: String? = null,
            val passwordError: String? = null
        ) : LoginState()
        data class CredentialsError(val message: String) : LoginState()
        data class Success(val user: User) : LoginState()
    }

    sealed class RegisterState {
        data object Idle : RegisterState()
        data object Loading : RegisterState()
        data class ValidationError(
            val usernameError: String? = null,
            val passwordError: String? = null,
            val confirmPasswordError: String? = null
        ) : RegisterState()
        data class DuplicateUsernameError(val message: String) : RegisterState()
        data class ServerError(val message: String) : RegisterState()
        data object Success : RegisterState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    /**
     * Memvalidasi dan memproses login pengguna (FR-04, FR-05).
     */
    fun login(username: String, password: String) {
        val cleanUsername = username.trim()
        val usernameErr = if (cleanUsername.isEmpty()) "Username wajib diisi" else null
        val passwordErr = if (password.isEmpty()) "Password wajib diisi" else null

        if (usernameErr != null || passwordErr != null) {
            _loginState.value = LoginState.ValidationError(
                usernameError = usernameErr,
                passwordError = passwordErr
            )
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            when (val outcome_187 = authRepository.authenticateWithOutcome(cleanUsername, password)) {
                is AuthRepository.LoginOutcome.Success -> {
                    _loginState.value = LoginState.Success(outcome_187.user_187)
                }
                is AuthRepository.LoginOutcome.InvalidCredentials -> {
                    _loginState.value = LoginState.CredentialsError(outcome_187.message_187)
                }
                is AuthRepository.LoginOutcome.Error -> {
                    _loginState.value = LoginState.CredentialsError(outcome_187.message_187)
                }
            }
        }
    }

    /**
     * Memvalidasi dan memproses registrasi pengguna baru (FR-01, FR-02, FR-03).
     */
    fun register(username: String, password: String, confirmPassword: String) {
        val cleanUsername = username.trim()
        val usernameErr = if (cleanUsername.isEmpty()) "Username wajib diisi" else null
        val passwordErr = when {
            password.isEmpty() -> "Password wajib diisi"
            password.length < MIN_PASSWORD_LENGTH -> "Password minimal 6 karakter"
            else -> null
        }
        val confirmErr = when {
            confirmPassword.isEmpty() -> "Konfirmasi password wajib diisi"
            password != confirmPassword -> "Konfirmasi password tidak sama"
            else -> null
        }

        if (usernameErr != null || passwordErr != null || confirmErr != null) {
            _registerState.value = RegisterState.ValidationError(
                usernameError = usernameErr,
                passwordError = passwordErr,
                confirmPasswordError = confirmErr
            )
            return
        }

        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            when (val outcome_187 = authRepository.registerUserWithOutcome(cleanUsername, password)) {
                is AuthRepository.RegisterOutcome.Success -> {
                    _registerState.value = RegisterState.Success
                }
                is AuthRepository.RegisterOutcome.DuplicateUsername -> {
                    _registerState.value = RegisterState.DuplicateUsernameError(outcome_187.message_187)
                }
                is AuthRepository.RegisterOutcome.Error -> {
                    _registerState.value = RegisterState.ServerError(outcome_187.message_187)
                }
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
