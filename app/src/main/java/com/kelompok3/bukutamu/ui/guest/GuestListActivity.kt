package com.kelompok3.bukutamu.ui.guest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelompok3.bukutamu.R
import com.kelompok3.bukutamu.data.AppDatabase
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.remote.BukuTamuApiClient
import com.kelompok3.bukutamu.data.repository.GuestRepository
import com.kelompok3.bukutamu.databinding.ActivityGuestListBinding
import com.kelompok3.bukutamu.ui.auth.LoginActivity
import com.kelompok3.bukutamu.ui.viewmodel.GuestViewModel
import com.kelompok3.bukutamu.util.SessionManager
import com.kelompok3.bukutamu.util.WindowInsetsHelper
import kotlinx.coroutines.launch

/**
 * Layar Daftar Tamu sesuai PRD 11.4 & FR-14 s.d. FR-17, FR-21, FR-22, FR-23.
 * Menyajikan header dengan Safe-Area Status Bar Inset, pencarian real-time,
 * kartu baris terkelompok dinamis (mengikuti jumlah item sebenarnya), dan Bottom Docked Action Bar.
 */
class GuestListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var guestAdapter: GuestAdapter

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
            redirectToLogin()
            return
        }

        binding = ActivityGuestListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsHelper.applyEdgeToEdgeInsets(
            activity = this,
            rootView = binding.layoutGuestListRoot,
            topBar = binding.headerContainer,
            bottomView = binding.bottomListBar,
            extraTopDp = 6,
            extraBottomDp = 12
        )

        setupToolbar()
        setupSearchInput()
        setupRecyclerView()
        setupBottomAction()
        observeGuestList()
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            viewModel.syncFromServer()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSearchInput() {
        val currentQuery = viewModel.searchQuery.value
        if (currentQuery.isNotEmpty() && binding.etSearchGuest.text.toString() != currentQuery) {
            binding.etSearchGuest.setText(currentQuery)
        }
        binding.btnClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE

        binding.etSearchGuest.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            viewModel.setSearchQuery(query)
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchGuest.text?.clear()
        }
    }

    private fun setupRecyclerView() {
        guestAdapter = GuestAdapter(
            onItemClick = { guest ->
                val intent = Intent(this, GuestFormActivity::class.java).apply {
                    putExtra(GuestFormActivity.EXTRA_GUEST_ID, guest.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { guest ->
                showDeleteConfirmationDialog(guest)
            }
        )

        binding.rvGuests.apply {
            layoutManager = LinearLayoutManager(this@GuestListActivity)
            adapter = guestAdapter
            itemAnimator = null
        }
    }

    private fun setupBottomAction() {
        binding.fabAddGuest.setOnClickListener {
            val intent = Intent(this, GuestFormActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeGuestList() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.guests.collect { guestList ->
                    guestAdapter.submitList(guestList) {
                        if (guestList.isNotEmpty()) {
                            guestAdapter.notifyItemRangeChanged(0, guestList.size)
                        }
                    }

                    binding.toolbar.subtitle = getString(R.string.subtitle_guest_count, guestList.size)

                    val isSearching = viewModel.searchQuery.value.trim().isNotEmpty()
                    if (guestList.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvGuests.visibility = View.GONE
                        if (isSearching) {
                            binding.tvEmptyStateTitle.text = getString(R.string.empty_search_result_title)
                            binding.tvEmptyState.text = getString(R.string.empty_search_result)
                        } else {
                            binding.tvEmptyStateTitle.text = getString(R.string.empty_guest_list_title)
                            binding.tvEmptyState.text = getString(R.string.empty_guest_list)
                        }
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvGuests.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(guest: Guest) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message))
            .setNegativeButton(getString(R.string.btn_no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.btn_yes)) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteGuest(guest.id) { deleted ->
                    if (deleted) {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_guest_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
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

    private fun redirectToLogin() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
