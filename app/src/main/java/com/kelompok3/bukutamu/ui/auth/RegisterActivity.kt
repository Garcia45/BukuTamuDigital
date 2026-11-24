package com.kelompok3.bukutamu.ui.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kelompok3.bukutamu.R
import com.kelompok3.bukutamu.data.AppDatabase
import com.kelompok3.bukutamu.data.remote.BukuTamuApiClient
import com.kelompok3.bukutamu.data.repository.AuthRepository
import com.kelompok3.bukutamu.databinding.ActivityRegisterBinding
import com.kelompok3.bukutamu.ui.viewmodel.AuthViewModel
import com.kelompok3.bukutamu.util.MotionUtils
import com.kelompok3.bukutamu.util.WindowInsetsHelper
import kotlinx.coroutines.launch

/**
 * Layar Register akun baru sesuai PRD 11.2 & FR-01 s.d. FR-03.
 * Menangani WindowInsets (Status Bar, Navigation Bar, & Keyboard IME) dengan header stabil.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val viewModel: AuthViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        AuthViewModel.Factory(AuthRepository(db.userDao(), BukuTamuApiClient()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsHelper.applyEdgeToEdgeInsets(
            activity = this,
            rootView = binding.layoutRegisterRoot,
            topBar = binding.headerRegisterBar,
            bottomView = binding.scrollRegister,
            extraTopDp = 6,
            extraBottomDp = 20
        )

        if (savedInstanceState == null) {
            MotionUtils.animateSurfaceEntry(binding.containerRegisterContent)
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBackToLogin.setOnClickListener {
            finish()
        }

        binding.etUsername.doAfterTextChanged {
            binding.tilUsername.error = null
            binding.layoutErrorBanner.visibility = View.GONE
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            binding.layoutErrorBanner.visibility = View.GONE
        }
        binding.etConfirmPassword.doAfterTextChanged {
            binding.tilConfirmPassword.error = null
            binding.layoutErrorBanner.visibility = View.GONE
        }

        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitRegister()
                true
            } else {
                false
            }
        }

        binding.btnRegister.setOnClickListener {
            submitRegister()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun submitRegister() {
        val username = binding.etUsername.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
        viewModel.register(username, password, confirmPassword)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is AuthViewModel.RegisterState.Idle -> {
                            setLoading(false)
                        }
                        is AuthViewModel.RegisterState.Loading -> {
                            setLoading(true)
                            binding.layoutErrorBanner.visibility = View.GONE
                        }
                        is AuthViewModel.RegisterState.ValidationError -> {
                            setLoading(false)
                            binding.tilUsername.error = state.usernameError
                            binding.tilPassword.error = state.passwordError
                            binding.tilConfirmPassword.error = state.confirmPasswordError
                        }
                        is AuthViewModel.RegisterState.DuplicateUsernameError -> {
                            setLoading(false)
                            binding.tilUsername.error = state.message
                            binding.tvErrorMessage.text = state.message
                            MotionUtils.revealStatusBanner(binding.layoutErrorBanner)
                        }
                        is AuthViewModel.RegisterState.ServerError -> {
                            setLoading(false)
                            binding.tvErrorMessage.text = state.message
                            MotionUtils.revealStatusBanner(binding.layoutErrorBanner)
                        }
                        is AuthViewModel.RegisterState.Success -> {
                            setLoading(false)
                            Toast.makeText(
                                this@RegisterActivity,
                                getString(R.string.msg_register_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetRegisterState()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.tvGoToLogin.isEnabled = !isLoading
        binding.btnBackToLogin.isEnabled = !isLoading
    }
}
