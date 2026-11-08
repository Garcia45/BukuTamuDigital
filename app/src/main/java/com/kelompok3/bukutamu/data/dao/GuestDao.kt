package com.kelompok3.bukutamu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kelompok3.bukutamu.data.entity.Guest
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk akses data Guest sesuai PRD 7.5.
 * Isolasi data: seluruh query wajib memfilter user_id milik user yang sedang login.
 */
@Dao
interface GuestDao {
    @Insert
    suspend fun insert(guest: Guest)

    @Update
    suspend fun update(guest: Guest)

    @Delete
    suspend fun delete(guest: Guest)

    @Query("SELECT * FROM guests WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllByUser(userId: Int): Flow<List<Guest>>

    @Query("SELECT * FROM guests WHERE user_id = :userId AND nama LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchByName(userId: Int, query: String): Flow<List<Guest>>

    @Query("SELECT * FROM guests WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getById(id: Int, userId: Int): Guest?

    @Query("SELECT COUNT(*) FROM guests WHERE user_id = :userId AND timestamp BETWEEN :start AND :end")
    fun countBetween(userId: Int, start: Long, end: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM guests WHERE user_id = :userId")
    fun countAllByUser(userId: Int): Flow<Int>
}
