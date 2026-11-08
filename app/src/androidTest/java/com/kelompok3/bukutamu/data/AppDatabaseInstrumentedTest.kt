package com.kelompok3.bukutamu.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kelompok3.bukutamu.data.dao.GuestDao
import com.kelompok3.bukutamu.data.dao.UserDao
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.entity.User
import com.kelompok3.bukutamu.data.repository.GuestRepository
import com.kelompok3.bukutamu.util.DateUtils
import com.kelompok3.bukutamu.util.PasswordHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test Room Database sesuai PRD bagian 15:
 * - Insert, update, delete Guest
 * - Query terfilter user_id (isolasi data antar user)
 * - ForeignKey CASCADE delete saat User dihapus
 * - Unique index pada username
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var guestDao: GuestDao
    private lateinit var guestRepository: GuestRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        guestDao = db.guestDao()
        guestRepository = GuestRepository(guestDao)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun uniqueUsername_rejectsDuplicateUsernameInsert() = runBlocking {
        val user1 = User(username = "petugas_utama", password = PasswordHasher.hashPassword("123456"))
        val id1 = userDao.insert(user1)
        assertTrue(id1 > 0)

        try {
            val duplicate = User(username = "petugas_utama", password = PasswordHasher.hashPassword("654321"))
            userDao.insert(duplicate)
            fail("Seharusnya melempar SQLiteConstraintException karena username duplikat")
        } catch (expected: SQLiteConstraintException) {
            // Sesuai ekspektasi unique index pada kolom username
        }
    }

    @Test
    fun insertUpdateDeleteGuest_andPreserveTimestampOnEdit() = runBlocking {
        val userId = userDao.insert(
            User(username = "user_crud", password = PasswordHasher.hashPassword("secret123"))
        ).toInt()

        // Create
        guestRepository.insertGuest(
            userId = userId,
            nama = "Andi Wijaya",
            alamat = "Bandung",
            keperluan = "Konsultasi"
        )

        val guestsAfterInsert = guestDao.getAllByUser(userId).first()
        assertEquals(1, guestsAfterInsert.size)
        val guest = guestsAfterInsert.first()
        val originalTimestamp = guest.timestamp

        // Update (timestamp tidak boleh berubah)
        val updated = guestRepository.updateGuest(
            guestId = guest.id,
            userId = userId,
            nama = "Andi Wijaya, S.T.",
            alamat = "Jakarta",
            keperluan = "Presentasi Proyek"
        )
        assertTrue(updated)

        val fetched = guestDao.getById(guest.id, userId)
        assertNotNull(fetched)
        assertEquals("Andi Wijaya, S.T.", fetched!!.nama)
        assertEquals("Jakarta", fetched.alamat)
        assertEquals("Presentasi Proyek", fetched.keperluan)
        assertEquals(originalTimestamp, fetched.timestamp)

        // Delete
        val deleted = guestRepository.deleteGuest(guest.id, userId)
        assertTrue(deleted)
        assertTrue(guestDao.getAllByUser(userId).first().isEmpty())
    }

    @Test
    fun dataIsolation_queriesFilterStrictlyByUserId() = runBlocking {
        val userAId = userDao.insert(
            User(username = "user_a", password = PasswordHasher.hashPassword("passA123"))
        ).toInt()
        val userBId = userDao.insert(
            User(username = "user_b", password = PasswordHasher.hashPassword("passB123"))
        ).toInt()

        guestRepository.insertGuest(userAId, "Tamu A1", "Alamat A1", "Keperluan A1")
        guestRepository.insertGuest(userAId, "Tamu A2", "Alamat A2", "Keperluan A2")
        guestRepository.insertGuest(userBId, "Tamu B1", "Alamat B1", "Keperluan B1")

        val listA = guestDao.getAllByUser(userAId).first()
        val listB = guestDao.getAllByUser(userBId).first()

        assertEquals(2, listA.size)
        assertTrue(listA.all { it.userId == userAId })

        assertEquals(1, listB.size)
        assertEquals("Tamu B1", listB.first().nama)

        // User B tidak dapat mengakses, mengedit, atau menghapus tamu milik User A
        val guestA1 = listA.first()
        assertNull(guestDao.getById(guestA1.id, userBId))
        assertFalse(guestRepository.updateGuest(guestA1.id, userBId, "Ubah", "Ubah", "Ubah"))
        assertFalse(guestRepository.deleteGuest(guestA1.id, userBId))
    }

    @Test
    fun foreignKeyCascade_deletingUserDeletesAllOwnedGuestsOnly() = runBlocking {
        val userAId = userDao.insert(
            User(username = "user_cascade_a", password = PasswordHasher.hashPassword("pass123"))
        ).toInt()
        val userBId = userDao.insert(
            User(username = "user_cascade_b", password = PasswordHasher.hashPassword("pass123"))
        ).toInt()

        guestRepository.insertGuest(userAId, "Tamu A1", "Alamat A1", "Keperluan A1")
        guestRepository.insertGuest(userAId, "Tamu A2", "Alamat A2", "Keperluan A2")
        guestRepository.insertGuest(userBId, "Tamu B1", "Alamat B1", "Keperluan B1")

        val userA = userDao.findByUsername("user_cascade_a")!!
        userDao.delete(userA)

        // Seluruh tamu milik User A harus ikut terhapus (CASCADE), sedangkan tamu User B tetap utuh
        assertTrue(guestDao.getAllByUser(userAId).first().isEmpty())
        assertEquals(1, guestDao.getAllByUser(userBId).first().size)
    }

    @Test
    fun countBetween_countsOnlyTodayGuestsForSpecificUser() = runBlocking {
        val userAId = userDao.insert(
            User(username = "user_rekap_a", password = PasswordHasher.hashPassword("pass123"))
        ).toInt()
        val userBId = userDao.insert(
            User(username = "user_rekap_b", password = PasswordHasher.hashPassword("pass123"))
        ).toInt()

        val now = System.currentTimeMillis()
        val yesterday = now - 24L * 60 * 60 * 1000L

        guestDao.insert(Guest(userId = userAId, nama = "A1", alamat = "L1", keperluan = "K1", timestamp = now - 2000))
        guestDao.insert(Guest(userId = userAId, nama = "A2", alamat = "L2", keperluan = "K2", timestamp = now - 1000))
        guestDao.insert(Guest(userId = userAId, nama = "A3", alamat = "L3", keperluan = "K3", timestamp = now))
        guestDao.insert(Guest(userId = userAId, nama = "A_Kemarin", alamat = "LK", keperluan = "KK", timestamp = yesterday))
        guestDao.insert(Guest(userId = userBId, nama = "B_HariIni", alamat = "LB", keperluan = "KB", timestamp = now))

        val (startOfDay, endOfDay) = DateUtils.getDayRange(now)
        val countTodayUserA = guestDao.countBetween(userAId, startOfDay, endOfDay).first()
        val countTodayUserB = guestDao.countBetween(userBId, startOfDay, endOfDay).first()

        assertEquals(3, countTodayUserA)
        assertEquals(1, countTodayUserB)
    }
}

