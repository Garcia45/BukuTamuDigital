package com.kelompok3.bukutamu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.repository.GuestRepository
import com.kelompok3.bukutamu.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel untuk Dashboard dan CRUD Tamu (FR-10 s.d. FR-26).
 * Seluruh operasi terikat pada userId dari sesi aktif untuk menjamin isolasi data.
 */
class GuestViewModel(
    private val guestRepository: GuestRepository,
    private val currentUserId: Int
) : ViewModel() {

    /**
     * Model data untuk riwayat rekapitulasi jumlah tamu harian (FR-26 Opsional).
     */
    data class DailyRecap(
        val dayStartMillis: Long,
        val dateText: String,
        val count: Int
    )

    sealed class FormState {
        data object Idle : FormState()
        data object Loading : FormState()
        data class Loaded(val guest: Guest) : FormState()
        data class ValidationError(
            val namaError: String? = null,
            val alamatError: String? = null,
            val keperluanError: String? = null
        ) : FormState()
        data class Error(val message: String) : FormState()
        data class Saved(val isEditMode: Boolean) : FormState()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Daftar tamu milik user yang sedang login, diurutkan dari terbaru dan mendukung
     * pencarian berdasarkan nama (FR-14, FR-16, FR-23 Opsional).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val guests: StateFlow<List<Guest>> = _searchQuery
        .flatMapLatest { query ->
            guestRepository.searchByName(currentUserId, query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val dayRangeFlow = MutableStateFlow(DateUtils.getDayRange())

    /**
     * Jumlah tamu hari ini (00:00:00 s.d. 23:59:59) milik user yang sedang login (FR-10, FR-24, FR-25).
     * Diperbarui secara reaktif menggunakan Flow setiap ada perubahan database.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayGuestCount: StateFlow<Int> = dayRangeFlow
        .flatMapLatest { (startOfDay, endOfDay) ->
            guestRepository.countBetween(currentUserId, startOfDay, endOfDay)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    /**
     * Total keseluruhan tamu milik user yang sedang login (FR-13 Opsional).
     */
    val totalGuestCount: StateFlow<Int> = guestRepository
        .countAllByUser(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    /**
     * Riwayat rekap jumlah tamu per tanggal berdasarkan zona waktu perangkat (FR-26 Opsional).
     */
    val dailyRecapHistory: StateFlow<List<DailyRecap>> = guestRepository
        .getAllByUser(currentUserId)
        .map { list ->
            list.groupBy { guest -> DateUtils.getTodayStartMillis(guest.timestamp) }
                .entries
                .sortedByDescending { it.key }
                .map { (dayStart, guestsOnDay) ->
                    DailyRecap(
                        dayStartMillis = dayStart,
                        dateText = DateUtils.formatDateOnly(dayStart),
                        count = guestsOnDay.size
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _loadedGuest = MutableStateFlow<Guest?>(null)
    val loadedGuest: StateFlow<Guest?> = _loadedGuest.asStateFlow()

    private val _formState = MutableStateFlow<FormState>(FormState.Idle)
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    /**
     * Mengubah kata kunci pencarian nama tamu (FR-23 Opsional).
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Memperbarui acuan rentang hari saat layar kembali aktif (misal melewati tengah malam).
     */
    fun refreshTodayRange() {
        dayRangeFlow.value = DateUtils.getDayRange()
    }

    /**
     * Memuat data tamu berdasarkan ID untuk mode edit dengan validasi kepemilikan (FR-20).
     */
    fun loadGuestForEdit(guestId: Int) {
        if (_loadedGuest.value?.id == guestId) return
        _formState.value = FormState.Loading
        viewModelScope.launch {
            val guest = guestRepository.getById(guestId, currentUserId)
            if (guest != null) {
                _loadedGuest.value = guest
                _formState.value = FormState.Loaded(guest)
            } else {
                _formState.value = FormState.Error("Data tamu tidak ditemukan atau bukan milik akun ini")
            }
        }
    }

    /**
     * Memvalidasi dan menyimpan data tamu (mode tambah maupun mode edit) (FR-18, FR-19, FR-20).
     */
    fun saveGuest(
        guestId: Int?,
        nama: String,
        alamat: String,
        keperluan: String
    ) {
        val cleanNama = nama.trim()
        val cleanAlamat = alamat.trim()
        val cleanKeperluan = keperluan.trim()

        val namaErr = if (cleanNama.isEmpty()) "Nama tamu wajib diisi" else null
        val alamatErr = if (cleanAlamat.isEmpty()) "Alamat wajib diisi" else null
        val keperluanErr = if (cleanKeperluan.isEmpty()) "Keperluan wajib diisi" else null

        if (namaErr != null || alamatErr != null || keperluanErr != null) {
            _formState.value = FormState.ValidationError(
                namaError = namaErr,
                alamatError = alamatErr,
                keperluanError = keperluanErr
            )
            return
        }

        _formState.value = FormState.Loading
        viewModelScope.launch {
            if (guestId == null || guestId <= 0) {
                guestRepository.insertGuest(
                    userId = currentUserId,
                    nama = cleanNama,
                    alamat = cleanAlamat,
                    keperluan = cleanKeperluan
                )
                refreshTodayRange()
                _formState.value = FormState.Saved(isEditMode = false)
            } else {
                val updated = guestRepository.updateGuest(
                    guestId = guestId,
                    userId = currentUserId,
                    nama = cleanNama,
                    alamat = cleanAlamat,
                    keperluan = cleanKeperluan
                )
                if (updated) {
                    refreshTodayRange()
                    _formState.value = FormState.Saved(isEditMode = true)
                } else {
                    _formState.value = FormState.Error("Gagal memperbarui: Data bukan milik akun ini")
                }
            }
        }
    }

    /**
     * Menghapus tamu milik user yang sedang login setelah konfirmasi (FR-21, FR-22).
     */
    fun deleteGuest(guestId: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val deleted = guestRepository.deleteGuest(guestId, currentUserId)
            if (deleted) {
                refreshTodayRange()
            }
            onResult(deleted)
        }
    }

    fun resetFormState() {
        _formState.value = FormState.Idle
    }

    class Factory(
        private val guestRepository: GuestRepository,
        private val currentUserId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GuestViewModel::class.java)) {
                return GuestViewModel(guestRepository, currentUserId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
