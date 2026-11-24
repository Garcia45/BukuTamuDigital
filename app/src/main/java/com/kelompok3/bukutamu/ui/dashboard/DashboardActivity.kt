package com.kelompok3.bukutamu.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelompok3.bukutamu.R
import com.kelompok3.bukutamu.data.AppDatabase
import com.kelompok3.bukutamu.data.remote.BukuTamuApiClient
import com.kelompok3.bukutamu.data.repository.GuestRepository
import com.kelompok3.bukutamu.databinding.ActivityDashboardBinding
import com.kelompok3.bukutamu.ui.auth.LoginActivity
import com.kelompok3.bukutamu.ui.guest.GuestFormActivity
import com.kelompok3.bukutamu.ui.guest.GuestListActivity
import com.kelompok3.bukutamu.ui.viewmodel.GuestViewModel
import com.kelompok3.bukutamu.util.MotionUtils
import com.kelompok3.bukutamu.util.SessionManager
import com.kelompok3.bukutamu.util.WindowInsetsHelper
import kotlinx.coroutines.launch

/**
 * Layar Dashboard sesuai PRD 11.3 & FR-09 s.d. FR-13, FR-24 s.d. FR-26.
 * Menerapkan komposisi mobile proporsional:
 * - Top Header Bar terpisah dari Status Bar (WindowInsets)
 * - Scrollable Body (Hero Metric Card, Menu Navigasi, & Riwayat Rekap Harian)
 * - Bottom Docked Primary Action Bar ("+ Catat Tamu Baru") di area jangkauan ibu jari bawah.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var dailyRecapAdapter: DailyRecapAdapter

    private val viewModel: GuestViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        GuestViewModel.Factory(
            guestRepository = GuestRepository(db.guestDao(), BukuTamuApiClient()),
            currentUserId = sessionManager.getUserId()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            performLogout()
            return
        }

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsHelper.applyEdgeToEdgeInsets(
            activity = this,
            rootView = binding.layoutDashboardRoot,
            topBar = binding.topHeaderBar,
            bottomView = binding.bottomActionBar,
            extraTopDp = 10,
            extraBottomDp = 12
        )

        if (savedInstanceState == null) {
            MotionUtils.animateSurfaceEntry(binding.layoutDashboardContainer)
        }

        setupUserGreeting()
        setupDailyRecapList()
        setupNavigationButtons()
        observeDashboardMetrics()
    }

    override fun onResume() {
        super.onResume()
        if (!sessionManager.isLoggedIn()) {
            performLogout()
            return
        }
        viewModel.refreshTodayRange()
        viewModel.syncFromServer()
    }

    private fun setupUserGreeting() {
        val username = sessionManager.getUsername()
        // FR-09: Menampilkan salam dengan username yang sedang login
        binding.tvGreeting.text = getString(R.string.greeting_user, username)
    }

    private fun setupDailyRecapList() {
        dailyRecapAdapter = DailyRecapAdapter()
        binding.rvDailyRecap.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = dailyRecapAdapter
            itemAnimator = null
        }
    }

    private fun setupNavigationButtons() {
        // FR-11: Tombol utama menuju Tambah Tamu dan baris navigasi ke Daftar Tamu
        binding.btnAddGuest.setOnClickListener {
            val intent = Intent(this, GuestFormActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewGuestList.setOnClickListener {
            val intent = Intent(this, GuestListActivity::class.java)
            startActivity(intent)
        }

        // FR-08 & FR-12: Tombol Keluar dengan konfirmasi eksplisit
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun observeDashboardMetrics() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // FR-10, FR-22, FR-24, FR-25: Rekap jumlah tamu hari ini secara reaktif via Flow
                launch {
                    var isFirstEmission = true
                    viewModel.todayGuestCount.collect { count ->
                        val newText = count.toString()
                        val changed = binding.tvTodayCount.text.toString() != newText
                        binding.tvTodayCount.text = newText
                        if (!isFirstEmission && changed) {
                            MotionUtils.animateMetricUpdate(binding.tvTodayCount)
                        }
                        isFirstEmission = false
                    }
                }

                // FR-13: Total keseluruhan kunjungan pada akun ini
                launch {
                    viewModel.totalGuestCount.collect { total ->
                        binding.tvTotalCount.text = getString(R.string.value_guest_count, total)
                    }
                }

                // FR-26: Riwayat rekap harian per tanggal
                launch {
                    viewModel.dailyRecapHistory.collect { history ->
                        dailyRecapAdapter.submitList(history)
                        if (history.isEmpty()) {
                            binding.layoutEmptyDailyHistory.visibility = View.VISIBLE
                            binding.rvDailyRecap.visibility = View.GONE
                        } else {
                            binding.layoutEmptyDailyHistory.visibility = View.GONE
                            binding.rvDailyRecap.visibility = View.VISIBLE
                        }
                    }
                }

                // Tampilkan pemberitahuan jika gagal sinkronisasi dengan server PHP
                launch {
                    viewModel.syncMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            Toast.makeText(this@DashboardActivity, message, Toast.LENGTH_SHORT).show()
                            viewModel.clearSyncMessage()
                        }
                    }
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.btn_logout)) { dialog, _ ->
                dialog.dismiss()
                performLogout()
            }
            .show()
    }

    private fun performLogout() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
