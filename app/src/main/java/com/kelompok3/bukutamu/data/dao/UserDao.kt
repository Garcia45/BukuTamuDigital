package com.kelompok3.bukutamu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kelompok3.bukutamu.data.entity.User

/**
 * DAO untuk akses data tabel users_187 (NIM 187) + dukungan cache sinkronisasi API.
 */
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user_187: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user_187: User): Long

    @Query("SELECT * FROM users_187 WHERE username = :username_187 LIMIT 1")
    suspend fun findByUsername(username_187: String): User?

    @Delete
    suspend fun delete(user_187: User)
}
