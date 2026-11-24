package com.kelompok3.bukutamu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kelompok3.bukutamu.data.dao.GuestDao
import com.kelompok3.bukutamu.data.dao.UserDao
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.entity.User

/**
 * Singleton Room Database untuk aplikasi Buku Tamu Digital (NIM 187).
 */
@Database(
    entities = [User::class, Guest::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun guestDao(): GuestDao

    companion object {
        private const val DATABASE_NAME_187 = "buku_tamu_digital_187.db"

        @Volatile
        private var INSTANCE_187: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE_187 ?: synchronized(this) {
                val instance_187 = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME_187
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE_187 = instance_187
                instance_187
            }
        }
    }
}
