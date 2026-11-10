package com.kelompok3.bukutamu.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kelompok3.bukutamu.data.AppDatabase
import com.kelompok3.bukutamu.data.repository.AuthRepository
import com.kelompok3.bukutamu.databinding.ActivityLoginBinding
import com.kelompok3.bukutamu.ui.dashboard.DashboardActivity
import com.kelompok3.bukutamu.ui.viewmodel.AuthViewModel
import com.kelompok3.bukutamu.util.MotionUtils
import com.kelompok3.bukutamu.util.SessionManager
import com.kelompok3.bukutamu.util.WindowInsetsHelper
import kotlinx.coroutines.launch

/**
 * Layar Login dan pengecekan sesi awal aplikasi sesuai PRD 11.1 & FR-04 s.d. FR-08.
 * Menangani WindowInsets (Status Bar, Navigation Bar, & Keyboard IME) serta pemusatan proporsional.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: AuthViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        AuthViewModel.Factory(AuthRepository(db.userDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // FR-07: Saat aplikasi dibuka, jika sesi aktif langsung ke Dashboard
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsHelper.applyEdgeToEdgeInsets(
            activity = this,
            rootView = binding.scrollLogin,
            topBar = binding.layoutLoginRoot,
            bottomView = binding.layoutLoginRoot,
            extraTopDp = 16,
            extraBottomDp = 20
        )

        if (savedInstanceState == null) {
            MotionUtils.animateSurfaceEntry(binding.containerLoginContent)
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.etUsername.doAfterTextChanged {
            binding.tilUsername.error = null
            binding.layoutErrorBanner.visibility = View.GONE
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            binding.layoutErrorBanner.visibility = View.GONE
        }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin()
                true
            } else {
                false
            }
        }

        binding.btnLogin.setOnClickListener {
            submitLogin()
        }

        binding.tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun submitLogin() {
        val username = binding.etUsername.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(username, password)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is AuthViewModel.LoginState.Idle -> {
                            setLoading(false)
                        }
                        is AuthViewModel.LoginState.Loading -> {
                            setLoading(true)
                            binding.layoutErrorBanner.visibility = View.GONE
                        }
                        is AuthViewModel.LoginState.ValidationError -> {
                            setLoading(false)
                            binding.tilUsername.error = state.usernameError
                            binding.tilPassword.error = state.passwordError
                        }
                        is AuthViewModel.LoginState.CredentialsError -> {
                            setLoading(false)
                            binding.tvErrorMessage.text = state.message
                            MotionUtils.revealStatusBanner(binding.layoutErrorBanner)
                        }
                        is AuthViewModel.LoginState.Success -> {
                            setLoading(false)
                            // FR-06: Simpan sesi di SharedPreferences
                            sessionManager.saveSession(
                                userId = state.user.id,
                                username = state.user.username
                            )
                            viewModel.resetLoginState()
                            navigateToDashboard()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.tvGoToRegister.isEnabled = !isLoading
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
