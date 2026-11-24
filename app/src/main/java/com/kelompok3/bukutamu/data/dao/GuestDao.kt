package com.kelompok3.bukutamu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kelompok3.bukutamu.data.entity.Guest
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk akses data tabel guests_187 (NIM 187) + dukungan sinkronisasi dari Backend PHP API.
 * Isolasi data: seluruh query wajib memfilter user_id milik user yang sedang login.
 */
@Dao
interface GuestDao {
    @Insert
    suspend fun insert(guest_187: Guest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(guest_187: Guest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guests_187: List<Guest>)

    @Update
    suspend fun update(guest_187: Guest)

    @Delete
    suspend fun delete(guest_187: Guest)

    @Query("DELETE FROM guests_187 WHERE user_id = :userId_187")
    suspend fun deleteAllByUser(userId_187: Int)

    @Transaction
    suspend fun replaceAllForUser(userId_187: Int, guests_187: List<Guest>) {
        deleteAllByUser(userId_187)
        if (guests_187.isNotEmpty()) {
            insertAll(guests_187)
        }
    }

    @Query("SELECT * FROM guests_187 WHERE user_id = :userId_187 ORDER BY timestamp DESC")
    fun getAllByUser(userId_187: Int): Flow<List<Guest>>

    @Query("SELECT * FROM guests_187 WHERE user_id = :userId_187 AND nama LIKE '%' || :query_187 || '%' ORDER BY timestamp DESC")
    fun searchByName(userId_187: Int, query_187: String): Flow<List<Guest>>

    @Query("SELECT * FROM guests_187 WHERE id = :id_187 AND user_id = :userId_187 LIMIT 1")
    suspend fun getById(id_187: Int, userId_187: Int): Guest?

    @Query("SELECT COUNT(*) FROM guests_187 WHERE user_id = :userId_187 AND timestamp BETWEEN :start_187 AND :end_187")
    fun countBetween(userId_187: Int, start_187: Long, end_187: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM guests_187 WHERE user_id = :userId_187")
    fun countAllByUser(userId_187: Int): Flow<Int>
}
