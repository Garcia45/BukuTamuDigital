package com.kelompok3.bukutamu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kelompok3.bukutamu.data.entity.User

/**
 * DAO untuk akses data User sesuai PRD 7.5.
 */
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?

    @Delete
    suspend fun delete(user: User)
}

