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
 * Singleton Room Database untuk aplikasi Buku Tamu Digital.
 */
@Database(
    entities = [User::class, Guest::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun guestDao(): GuestDao

    companion object {
        private const val DATABASE_NAME = "buku_tamu_digital.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

