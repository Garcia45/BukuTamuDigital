package com.kelompok3.bukutamu.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity untuk tabel users_187 (NIM 187) sesuai PRD 7.1 & 7.4.
 * Kolom username diberi unique index agar tidak ada duplikasi akun.
 */
@Entity(
    tableName = "users_187",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String
)
