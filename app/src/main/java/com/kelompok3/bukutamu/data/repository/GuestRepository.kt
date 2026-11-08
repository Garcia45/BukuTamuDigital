package com.kelompok3.bukutamu.data.repository

import com.kelompok3.bukutamu.data.dao.GuestDao
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.util.DateUtils
import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk pengelolaan data tamu.
 * Menjamin isolasi data: setiap operasi wajib menyertakan userId dari sesi aktif
 * dan memvalidasi kepemilikan sebelum mengubah atau menghapus data.
 */
class GuestRepository(private val guestDao: GuestDao) {

    /**
     * Mengambil daftar tamu milik user tertentu, diurutkan dari yang terbaru (FR-14, FR-16).
     */
    fun getAllByUser(userId: Int): Flow<List<Guest>> {
        return guestDao.getAllByUser(userId)
    }

    /**
     * Mencari tamu berdasarkan nama khusus milik user yang sedang login (FR-23 Opsional).
     */
    fun searchByName(userId: Int, query: String): Flow<List<Guest>> {
        val cleanQuery = query.trim()
        return if (cleanQuery.isEmpty()) {
            guestDao.getAllByUser(userId)
        } else {
            guestDao.searchByName(userId, cleanQuery)
        }
    }

    /**
     * Mengambil detail satu tamu dengan memvalidasi kepemilikan (id dan userId).
     */
    suspend fun getById(id: Int, userId: Int): Guest? {
        return guestDao.getById(id, userId)
    }

    /**
     * Menghitung jumlah tamu milik user pada rentang timestamp tertentu (FR-24, FR-25).
     */
    fun countBetween(userId: Int, start: Long, end: Long): Flow<Int> {
        return guestDao.countBetween(userId, start, end)
    }

    /**
     * Menghitung jumlah tamu hari ini (00:00:00.000 s.d. 23:59:59.999 waktu perangkat)
     * khusus untuk user yang sedang login (FR-10, FR-24, FR-25).
     */
    fun countTodayByUser(userId: Int): Flow<Int> {
        val (startOfDay, endOfDay) = DateUtils.getDayRange()
        return guestDao.countBetween(userId, startOfDay, endOfDay)
    }

    /**
     * Menghitung total keseluruhan tamu milik user yang sedang login (FR-13 Opsional).
     */
    fun countAllByUser(userId: Int): Flow<Int> {
        return guestDao.countAllByUser(userId)
    }

    /**
     * Menambahkan tamu baru (FR-18, FR-19).
     * userId berasal dari sesi dan timestamp diisi otomatis dengan System.currentTimeMillis().
     */
    suspend fun insertGuest(
        userId: Int,
        nama: String,
        alamat: String,
        keperluan: String
    ) {
        val guest = Guest(
            userId = userId,
            nama = nama.trim(),
            alamat = alamat.trim(),
            keperluan = keperluan.trim(),
            timestamp = System.currentTimeMillis()
        )
        guestDao.insert(guest)
    }

    /**
     * Memperbarui data tamu setelah memvalidasi kepemilikan (userId) (FR-20).
     * Timestamp asli dipertahankan dan tidak pernah diubah saat edit.
     *
     * @return true jika data ditemukan milik userId dan berhasil diperbarui, false jika tidak berhak/tidak ditemukan.
     */
    suspend fun updateGuest(
        guestId: Int,
        userId: Int,
        nama: String,
        alamat: String,
        keperluan: String
    ): Boolean {
        val existingGuest = guestDao.getById(guestId, userId) ?: return false
        if (existingGuest.userId != userId) {
            return false
        }
        val updatedGuest = existingGuest.copy(
            nama = nama.trim(),
            alamat = alamat.trim(),
            keperluan = keperluan.trim()
            // timestamp tetap menggunakan existingGuest.timestamp
        )
        guestDao.update(updatedGuest)
        return true
    }

    /**
     * Menghapus data tamu setelah memvalidasi kepemilikan (userId) (FR-21).
     *
     * @return true jika data ditemukan milik userId dan berhasil dihapus, false jika tidak berhak/tidak ditemukan.
     */
    suspend fun deleteGuest(guestId: Int, userId: Int): Boolean {
        val existingGuest = guestDao.getById(guestId, userId) ?: return false
        if (existingGuest.userId != userId) {
            return false
        }
        guestDao.delete(existingGuest)
        return true
    }
}
