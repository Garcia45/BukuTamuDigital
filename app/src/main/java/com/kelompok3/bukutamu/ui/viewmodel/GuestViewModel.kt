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
 * ViewModel untuk Dashboard dan CRUD Tamu (FR-10 s.d. FR-26) dengan sinkronisasi Backend PHP API.
 * Seluruh operasi terikat pada userId dari sesi aktif untuk menjamin isolasi data.
 */
class GuestViewModel(
    private val guestRepository: GuestRepository,
    private val currentUserId: Int
) : ViewModel() {

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

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

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

    val totalGuestCount: StateFlow<Int> = guestRepository
        .countAllByUser(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshTodayRange() {
        dayRangeFlow.value = DateUtils.getDayRange()
    }

    /**
     * Mengambil data tamu terbaru milik user dari Backend PHP API ke cache lokal Room.
     */
    fun syncFromServer() {
        viewModelScope.launch {
            val error = guestRepository.syncGuestsFromServer(currentUserId)
            _syncMessage.value = error
            refreshTodayRange()
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

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
                val error_187 = guestRepository.insertGuestWithError(
                    userId_187 = currentUserId,
                    nama_187 = cleanNama,
                    alamat_187 = cleanAlamat,
                    keperluan_187 = cleanKeperluan
                )
                if (error_187 == null) {
                    refreshTodayRange()
                    _formState.value = FormState.Saved(isEditMode = false)
                } else {
                    _formState.value = FormState.Error(error_187)
                }
            } else {
                val error_187 = guestRepository.updateGuestWithError(
                    guestId_187 = guestId,
                    userId_187 = currentUserId,
                    nama_187 = cleanNama,
                    alamat_187 = cleanAlamat,
                    keperluan_187 = cleanKeperluan
                )
                if (error_187 == null) {
                    refreshTodayRange()
                    _formState.value = FormState.Saved(isEditMode = true)
                } else {
                    _formState.value = FormState.Error(error_187)
                }
            }
        }
    }

    fun deleteGuest(guestId: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val error = guestRepository.deleteGuestWithError(guestId, currentUserId)
            if (error == null) {
                refreshTodayRange()
                onResult(true)
            } else {
                _syncMessage.value = error
                onResult(false)
            }
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
