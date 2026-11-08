package com.kelompok3.bukutamu.ui.guest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelompok3.bukutamu.R
import com.kelompok3.bukutamu.data.AppDatabase
import com.kelompok3.bukutamu.data.repository.GuestRepository
import com.kelompok3.bukutamu.databinding.ActivityGuestFormBinding
import com.kelompok3.bukutamu.ui.auth.LoginActivity
import com.kelompok3.bukutamu.ui.viewmodel.GuestViewModel
import com.kelompok3.bukutamu.util.DateUtils
import com.kelompok3.bukutamu.util.MotionUtils
import com.kelompok3.bukutamu.util.SessionManager
import kotlinx.coroutines.launch

/**
 * Layar Form Tambah dan Edit Tamu sesuai PRD 11.5 & FR-18 s.d. FR-22.
 * Dipakai ulang untuk mode Tambah dan mode Edit.
 * Menerapkan perlindungan terhadap perubahan yang belum disimpan (Modality & Return Paths - patterns-components.md),
 * menjaga input saat rotasi layar (AC-14), dan mempertahankan timestamp asli saat edit (FR-20).
 */
class GuestFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestFormBinding
    private lateinit var sessionManager: SessionManager

    private var guestId: Int = INVALID_GUEST_ID
    private val isEditMode: Boolean
        get() = guestId > 0

    // Mencegah data awal dari database menimpa input pengguna setelah rotasi layar (AC-14)
    private var isInitialDataPopulated: Boolean = false

    // Baseline nilai awal untuk mendeteksi apakah pengguna sudah mengubah isi form
    private var baselineNama: String = ""
    private var baselineAlamat: String = ""
    private var baselineKeperluan: String = ""

    private val viewModel: GuestViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        GuestViewModel.Factory(
            guestRepository = GuestRepository(db.guestDao()),
            currentUserId = sessionManager.getUserId()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        binding = ActivityGuestFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guestId = intent.getIntExtra(EXTRA_GUEST_ID, INVALID_GUEST_ID)
        isInitialDataPopulated = savedInstanceState?.getBoolean(KEY_INITIAL_DATA_POPULATED, false) ?: false
        baselineNama = savedInstanceState?.getString(KEY_BASELINE_NAMA, "") ?: ""
        baselineAlamat = savedInstanceState?.getString(KEY_BASELINE_ALAMAT, "") ?: ""
        baselineKeperluan = savedInstanceState?.getString(KEY_BASELINE_KEPERLUAN, "") ?: ""

        if (savedInstanceState == null) {
            MotionUtils.animateSurfaceEntry(binding.containerFormContent)
        }

        setupModeUi()
        setupListeners()
        setupBackNavigationProtection()
        observeFormState()

        if (isEditMode) {
            viewModel.loadGuestForEdit(guestId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_INITIAL_DATA_POPULATED, isInitialDataPopulated)
        outState.putString(KEY_BASELINE_NAMA, baselineNama)
        outState.putString(KEY_BASELINE_ALAMAT, baselineAlamat)
        outState.putString(KEY_BASELINE_KEPERLUAN, baselineKeperluan)
    }

    private fun setupModeUi() {
        binding.toolbar.setNavigationOnClickListener {
            handleAttemptDismiss()
        }

        if (isEditMode) {
            binding.toolbar.title = getString(R.string.title_edit_guest)
            binding.tvFormSubtitle.text = getString(R.string.subtitle_form_edit)
            binding.btnSave.text = getString(R.string.btn_save_changes)
            binding.btnDeleteInForm.visibility = View.VISIBLE
        } else {
            binding.toolbar.title = getString(R.string.title_add_guest)
            binding.tvFormSubtitle.text = getString(R.string.subtitle_form_add)
            binding.btnSave.text = getString(R.string.btn_save)
            binding.layoutVisitTimeBanner.visibility = View.GONE
            binding.btnDeleteInForm.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.etNama.doAfterTextChanged {
            binding.tilNama.error = null
            binding.layoutFormErrorBanner.visibility = View.GONE
        }
        binding.etAlamat.doAfterTextChanged {
            binding.tilAlamat.error = null
            binding.layoutFormErrorBanner.visibility = View.GONE
        }
        binding.etKeperluan.doAfterTextChanged {
            binding.tilKeperluan.error = null
            binding.layoutFormErrorBanner.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            val nama = binding.etNama.text?.toString().orEmpty()
            val alamat = binding.etAlamat.text?.toString().orEmpty()
            val keperluan = binding.etKeperluan.text?.toString().orEmpty()

            viewModel.saveGuest(
                guestId = if (isEditMode) guestId else null,
                nama = nama,
                alamat = alamat,
                keperluan = keperluan
            )
        }

        binding.btnCancel.setOnClickListener {
            handleAttemptDismiss()
        }

        binding.btnDeleteInForm.setOnClickListener {
            if (isEditMode) {
                showDeleteConfirmationDialog()
            }
        }
    }

    /**
     * Melindungi pengguna dari kehilangan input secara tidak sengaja saat menekan tombol Kembali atau Batal
     * (patterns-components.md: Modality and return paths).
     */
    private fun setupBackNavigationProtection() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleAttemptDismiss()
                }
            }
        )
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentNama = binding.etNama.text?.toString().orEmpty().trim()
        val currentAlamat = binding.etAlamat.text?.toString().orEmpty().trim()
        val currentKeperluan = binding.etKeperluan.text?.toString().orEmpty().trim()
        return currentNama != baselineNama ||
            currentAlamat != baselineAlamat ||
            currentKeperluan != baselineKeperluan
    }

    private fun handleAttemptDismiss() {
        if (hasUnsavedChanges()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_discard_title))
                .setMessage(getString(R.string.dialog_discard_message))
                .setNegativeButton(getString(R.string.btn_keep_editing)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.btn_discard)) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .show()
        } else {
            finish()
        }
    }

    private fun observeFormState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loadedGuest.collect { guest ->
                        if (guest != null) {
                            // Waktu kunjungan ditampilkan sebagai teks read-only pada mode edit (PRD 11.5)
                            binding.tvVisitTimeInfo.text = getString(
                                R.string.label_visit_time,
                                DateUtils.formatTimestamp(guest.timestamp)
                            )
                            binding.layoutVisitTimeBanner.visibility = View.VISIBLE

                            // Hanya isi field teks saat pertama kali dibuka agar perubahan saat rotasi layar tidak hilang
                            if (!isInitialDataPopulated) {
                                baselineNama = guest.nama.trim()
                                baselineAlamat = guest.alamat.trim()
                                baselineKeperluan = guest.keperluan.trim()
                                binding.etNama.setText(guest.nama)
                                binding.etAlamat.setText(guest.alamat)
                                binding.etKeperluan.setText(guest.keperluan)
                                isInitialDataPopulated = true
                            }
                        }
                    }
                }

                launch {
                    viewModel.formState.collect { state ->
                        when (state) {
                            is GuestViewModel.FormState.Idle -> {
                                setButtonsEnabled(true)
                            }
                            is GuestViewModel.FormState.Loading -> {
                                setButtonsEnabled(false)
                            }
                            is GuestViewModel.FormState.Loaded -> {
                                setButtonsEnabled(true)
                            }
                            is GuestViewModel.FormState.ValidationError -> {
                                setButtonsEnabled(true)
                                binding.tilNama.error = state.namaError
                                binding.tilAlamat.error = state.alamatError
                                binding.tilKeperluan.error = state.keperluanError
                            }
                            is GuestViewModel.FormState.Error -> {
                                setButtonsEnabled(true)
                                binding.tvFormError.text = state.message
                                MotionUtils.revealStatusBanner(binding.layoutFormErrorBanner)
                            }
                            is GuestViewModel.FormState.Saved -> {
                                setButtonsEnabled(true)
                                val msgRes = if (state.isEditMode) {
                                    R.string.msg_guest_updated
                                } else {
                                    R.string.msg_guest_saved
                                }
                                Toast.makeText(this@GuestFormActivity, getString(msgRes), Toast.LENGTH_SHORT).show()
                                viewModel.resetFormState()
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message))
            .setNegativeButton(getString(R.string.btn_no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.btn_yes)) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteGuest(guestId) { deleted ->
                    if (deleted) {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_guest_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.error_unauthorized_action),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnCancel.isEnabled = enabled
        binding.btnDeleteInForm.isEnabled = enabled
    }

    private fun redirectToLogin() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_GUEST_ID = "extra_guest_id"
        private const val INVALID_GUEST_ID = -1
        private const val KEY_INITIAL_DATA_POPULATED = "key_initial_data_populated"
        private const val KEY_BASELINE_NAMA = "key_baseline_nama"
        private const val KEY_BASELINE_ALAMAT = "key_baseline_alamat"
        private const val KEY_BASELINE_KEPERLUAN = "key_baseline_keperluan"
    }
}

