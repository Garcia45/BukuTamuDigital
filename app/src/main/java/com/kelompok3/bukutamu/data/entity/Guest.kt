package com.kelompok3.bukutamu.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity untuk tabel guests sesuai PRD 7.2 & 7.4.
 * Memiliki relasi ForeignKey ke User.id dengan onDelete = CASCADE dan index pada user_id.
 */
@Entity(
    tableName = "guests",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class Guest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val nama: String,
    val alamat: String,
    val keperluan: String,
    val timestamp: Long = System.currentTimeMillis()
)

