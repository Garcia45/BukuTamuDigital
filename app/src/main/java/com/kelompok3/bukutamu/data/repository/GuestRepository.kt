package com.kelompok3.bukutamu.data.repository

import com.kelompok3.bukutamu.data.dao.GuestDao
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.remote.ApiResult
import com.kelompok3.bukutamu.data.remote.BukuTamuApiService
import com.kelompok3.bukutamu.util.DateUtils
import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk pengelolaan data tamu pada tabel guests_187 (NIM 187).
 * Menggunakan Room Flow sebagai sumber reaktif UI dan menyinkronkan setiap operasi CRUD
 * dengan Backend PHP REST API apabila apiService_187 dikonfigurasi.
 */
class GuestRepository(
    private val guestDao_187: GuestDao,
    private val apiService_187: BukuTamuApiService? = null
) {

    fun getAllByUser(userId_187: Int): Flow<List<Guest>> {
        return guestDao_187.getAllByUser(userId_187)
    }

    fun searchByName(userId_187: Int, query_187: String): Flow<List<Guest>> {
        val cleanQuery_187 = query_187.trim()
        return if (cleanQuery_187.isEmpty()) {
            guestDao_187.getAllByUser(userId_187)
        } else {
            guestDao_187.searchByName(userId_187, cleanQuery_187)
        }
    }

    suspend fun getById(id_187: Int, userId_187: Int): Guest? {
        return guestDao_187.getById(id_187, userId_187)
    }

    fun countBetween(userId_187: Int, start_187: Long, end_187: Long): Flow<Int> {
        return guestDao_187.countBetween(userId_187, start_187, end_187)
    }

    fun countTodayByUser(userId_187: Int): Flow<Int> {
        val (startOfDay_187, endOfDay_187) = DateUtils.getDayRange()
        return guestDao_187.countBetween(userId_187, startOfDay_187, endOfDay_187)
    }

    fun countAllByUser(userId_187: Int): Flow<Int> {
        return guestDao_187.countAllByUser(userId_187)
    }

    suspend fun syncGuestsFromServer(userId_187: Int): String? {
        val remote_187 = apiService_187 ?: return null
        return when (val result_187 = remote_187.getGuests(userId_187)) {
            is ApiResult.Success -> {
                guestDao_187.replaceAllForUser(userId_187, result_187.data_187)
                null
            }
            is ApiResult.HttpError -> result_187.message_187
            is ApiResult.NetworkError -> result_187.message_187
        }
    }

    suspend fun insertGuestWithError(
        userId_187: Int,
        nama_187: String,
        alamat_187: String,
        keperluan_187: String
    ): String? {
        val cleanNama_187 = nama_187.trim()
        val cleanAlamat_187 = alamat_187.trim()
        val cleanKeperluan_187 = keperluan_187.trim()

        if (apiService_187 != null) {
            return when (
                val result_187 = apiService_187.createGuest(
                    userId_187,
                    cleanNama_187,
                    cleanAlamat_187,
                    cleanKeperluan_187
                )
            ) {
                is ApiResult.Success -> {
                    guestDao_187.upsert(result_187.data_187)
                    null
                }
                is ApiResult.HttpError -> result_187.message_187
                is ApiResult.NetworkError -> result_187.message_187
            }
        }

        val guest_187 = Guest(
            userId = userId_187,
            nama = cleanNama_187,
            alamat = cleanAlamat_187,
            keperluan = cleanKeperluan_187,
            timestamp = System.currentTimeMillis()
        )
        guestDao_187.insert(guest_187)
        return null
    }

    suspend fun insertGuest(
        userId: Int,
        nama: String,
        alamat: String,
        keperluan: String
    ) {
        insertGuestWithError(userId, nama, alamat, keperluan)
    }

    suspend fun updateGuestWithError(
        guestId_187: Int,
        userId_187: Int,
        nama_187: String,
        alamat_187: String,
        keperluan_187: String
    ): String? {
        val cleanNama_187 = nama_187.trim()
        val cleanAlamat_187 = alamat_187.trim()
        val cleanKeperluan_187 = keperluan_187.trim()

        if (apiService_187 != null) {
            return when (
                val result_187 = apiService_187.updateGuest(
                    guestId_187 = guestId_187,
                    userId_187 = userId_187,
                    nama_187 = cleanNama_187,
                    alamat_187 = cleanAlamat_187,
                    keperluan_187 = cleanKeperluan_187
                )
            ) {
                is ApiResult.Success -> {
                    guestDao_187.upsert(result_187.data_187)
                    null
                }
                is ApiResult.HttpError -> result_187.message_187
                is ApiResult.NetworkError -> result_187.message_187
            }
        }

        val existingGuest_187 = guestDao_187.getById(guestId_187, userId_187)
            ?: return "Gagal memperbarui: Data bukan milik akun ini"
        if (existingGuest_187.userId != userId_187) {
            return "Gagal memperbarui: Data bukan milik akun ini"
        }
        val updatedGuest_187 = existingGuest_187.copy(
            nama = cleanNama_187,
            alamat = cleanAlamat_187,
            keperluan = cleanKeperluan_187
        )
        guestDao_187.update(updatedGuest_187)
        return null
    }

    suspend fun updateGuest(
        guestId: Int,
        userId: Int,
        nama: String,
        alamat: String,
        keperluan: String
    ): Boolean {
        return updateGuestWithError(guestId, userId, nama, alamat, keperluan) == null
    }

    suspend fun deleteGuestWithError(guestId_187: Int, userId_187: Int): String? {
        if (apiService_187 != null) {
            return when (val result_187 = apiService_187.deleteGuest(guestId_187, userId_187)) {
                is ApiResult.Success -> {
                    val existing_187 = guestDao_187.getById(guestId_187, userId_187)
                    if (existing_187 != null) {
                        guestDao_187.delete(existing_187)
                    }
                    null
                }
                is ApiResult.HttpError -> result_187.message_187
                is ApiResult.NetworkError -> result_187.message_187
            }
        }

        val existingGuest_187 = guestDao_187.getById(guestId_187, userId_187)
            ?: return "Akses ditolak: Data ini bukan milik akun yang sedang aktif."
        if (existingGuest_187.userId != userId_187) {
            return "Akses ditolak: Data ini bukan milik akun yang sedang aktif."
        }
        guestDao_187.delete(existingGuest_187)
        return null
    }

    suspend fun deleteGuest(guestId: Int, userId: Int): Boolean {
        return deleteGuestWithError(guestId, userId) == null
    }
}
