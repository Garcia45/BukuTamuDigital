package com.kelompok3.bukutamu.ui.viewmodel

import com.kelompok3.bukutamu.data.dao.GuestDao
import com.kelompok3.bukutamu.data.dao.UserDao
import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.entity.User
import com.kelompok3.bukutamu.data.remote.ApiResult
import com.kelompok3.bukutamu.data.remote.BukuTamuApiService
import com.kelompok3.bukutamu.data.repository.AuthRepository
import com.kelompok3.bukutamu.data.repository.GuestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk memverifikasi kriteria penerimaan AC-01 s.d. AC-13 pada ViewModel & Repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelAndRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeGuestDao: FakeGuestDao
    private lateinit var authRepository: AuthRepository
    private lateinit var guestRepository: GuestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUserDao = FakeUserDao()
        fakeGuestDao = FakeGuestDao()
        authRepository = AuthRepository(fakeUserDao)
        guestRepository = GuestRepository(fakeGuestDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun ac01_and_ac02_registerNewAndDuplicateUsername() = runTest {
        val authViewModel = AuthViewModel(authRepository)

        // AC-01: Register dengan username baru
        authViewModel.register("resepsionis1", "rahasia123", "rahasia123")
        advanceUntilIdle()
        assertTrue(authViewModel.registerState.value is AuthViewModel.RegisterState.Success)

        // AC-02: Register dengan username yang sudah ada
        authViewModel.register("  resepsionis1  ", "rahasia123", "rahasia123")
        advanceUntilIdle()
        assertTrue(authViewModel.registerState.value is AuthViewModel.RegisterState.DuplicateUsernameError)
    }

    @Test
    fun fr03_registerValidation_rejectsEmptyShortOrMismatchedPasswords() = runTest {
        val authViewModel = AuthViewModel(authRepository)

        authViewModel.register("", "123", "456")
        val state = authViewModel.registerState.value
        assertTrue(state is AuthViewModel.RegisterState.ValidationError)
        val validation = state as AuthViewModel.RegisterState.ValidationError
        assertNotNull(validation.usernameError)
        assertNotNull(validation.passwordError)
        assertNotNull(validation.confirmPasswordError)
    }

    @Test
    fun ac03_and_ac04_loginWithCorrectAndWrongCredentials() = runTest {
        authRepository.registerUser("satpam_pagi", "aman1234")
        val authViewModel = AuthViewModel(authRepository)

        // AC-04: Login dengan password salah
        authViewModel.login("satpam_pagi", "salah999")
        advanceUntilIdle()
        val wrongState = authViewModel.loginState.value
        assertTrue(wrongState is AuthViewModel.LoginState.CredentialsError)
        assertEquals(
            "Username atau password salah",
            (wrongState as AuthViewModel.LoginState.CredentialsError).message
        )

        // AC-03: Login dengan kredensial benar
        authViewModel.login("satpam_pagi", "aman1234")
        advanceUntilIdle()
        val successState = authViewModel.loginState.value
        assertTrue(successState is AuthViewModel.LoginState.Success)
        assertEquals("satpam_pagi", (successState as AuthViewModel.LoginState.Success).user.username)
    }

    @Test
    fun ac07_ac08_ac09_ac10_crudGuestAndPreserveTimestampOnEdit() = runTest {
        val userId = 1
        val viewModel = GuestViewModel(guestRepository, userId)

        // AC-08: Tambah tamu dengan field kosong -> muncul validasi, data tidak tersimpan
        viewModel.saveGuest(guestId = null, nama = "  ", alamat = "", keperluan = "")
        assertTrue(viewModel.formState.value is GuestViewModel.FormState.ValidationError)
        assertEquals(0, guestRepository.getAllByUser(userId).first().size)

        // AC-07: Tambah tamu dengan semua field terisi -> data muncul dengan waktu otomatis
        viewModel.saveGuest(
            guestId = null,
            nama = "Budi Santoso",
            alamat = "Jl. Merdeka No. 10",
            keperluan = "Koordinasi Akademik"
        )
        advanceUntilIdle()
        assertTrue(viewModel.formState.value is GuestViewModel.FormState.Saved)

        val listAfterInsert = guestRepository.getAllByUser(userId).first()
        assertEquals(1, listAfterInsert.size)
        val insertedGuest = listAfterInsert.first()
        val originalTimestamp = insertedGuest.timestamp
        assertTrue(originalTimestamp > 0L)

        // AC-09: Edit data tamu -> perubahan tersimpan, timestamp tetap
        viewModel.saveGuest(
            guestId = insertedGuest.id,
            nama = "Budi Santoso, M.Kom.",
            alamat = "Jl. Merdeka No. 12",
            keperluan = "Rapat Kurikulum"
        )
        advanceUntilIdle()

        val updatedGuest = guestRepository.getById(insertedGuest.id, userId)
        assertNotNull(updatedGuest)
        assertEquals("Budi Santoso, M.Kom.", updatedGuest!!.nama)
        assertEquals("Jl. Merdeka No. 12", updatedGuest.alamat)
        assertEquals("Rapat Kurikulum", updatedGuest.keperluan)
        assertEquals(originalTimestamp, updatedGuest.timestamp)

        // AC-10: Hapus tamu -> data hilang dari list
        var deleteResult = false
        viewModel.deleteGuest(insertedGuest.id) { deleteResult = it }
        advanceUntilIdle()
        assertTrue(deleteResult)
        assertEquals(0, guestRepository.getAllByUser(userId).first().size)
    }

    @Test
    fun ac11_dataIsolationBetweenUserAAndUserB() = runTest {
        val userAId = 10
        val userBId = 20

        guestRepository.insertGuest(userAId, "Tamu A1", "Alamat A1", "Keperluan A1")
        guestRepository.insertGuest(userAId, "Tamu A2", "Alamat A2", "Keperluan A2")

        // AC-11: Login sebagai User B setelah User A menambah data -> User B tidak melihat data User A
        val userBGuests = guestRepository.getAllByUser(userBId).first()
        assertTrue(userBGuests.isEmpty())

        // User B juga tidak bisa mengubah atau menghapus data milik User A
        val guestA = guestRepository.getAllByUser(userAId).first().first()
        assertNull(guestRepository.getById(guestA.id, userBId))
        assertFalse(guestRepository.updateGuest(guestA.id, userBId, "Hack", "Hack", "Hack"))
        assertFalse(guestRepository.deleteGuest(guestA.id, userBId))
    }

    @Test
    fun ac12_and_ac13_todayRecapCountsOnlyTodayGuestsForLoggedInUser() = runTest {
        val userAId = 1
        val userBId = 2
        val now = System.currentTimeMillis()
        val yesterday = now - 24L * 60 * 60 * 1000L

        // AC-12: Tambah 3 tamu hari ini untuk User A
        fakeGuestDao.insert(Guest(userId = userAId, nama = "T1", alamat = "A1", keperluan = "K1", timestamp = now - 1000))
        fakeGuestDao.insert(Guest(userId = userAId, nama = "T2", alamat = "A2", keperluan = "K2", timestamp = now - 500))
        fakeGuestDao.insert(Guest(userId = userAId, nama = "T3", alamat = "A3", keperluan = "K3", timestamp = now))

        // AC-13: Tamu bertanggal kemarin tidak dihitung pada rekap hari ini
        fakeGuestDao.insert(Guest(userId = userAId, nama = "Tamu Kemarin", alamat = "AK", keperluan = "KK", timestamp = yesterday))

        // Tamu milik User B hari ini juga tidak ikut dihitung pada rekap User A
        fakeGuestDao.insert(Guest(userId = userBId, nama = "Tamu B", alamat = "AB", keperluan = "KB", timestamp = now))

        val todayCountUserA = guestRepository.countTodayByUser(userAId).first()
        assertEquals(3, todayCountUserA)

        // FR-13 & FR-23: Total keseluruhan tamu User A (4 tamu) dan pencarian berdasarkan nama
        assertEquals(4, guestRepository.countAllByUser(userAId).first())
        val searchResult = guestRepository.searchByName(userAId, "Kemarin").first()
        assertEquals(1, searchResult.size)
        assertEquals("Tamu Kemarin", searchResult.first().nama)
    }

    @Test
    fun remoteApiSync_syncsGuestsAndCrudFromPhpBackend() = runTest {
        val remoteGuest = Guest(
            id = 99,
            userId = 7,
            nama = "Siti Rahma",
            alamat = "Kantor Pusat",
            keperluan = "Audit Sistem",
            timestamp = System.currentTimeMillis()
        )
        val fakeApi_187 = object : BukuTamuApiService {
            private val store_187 = mutableListOf(remoteGuest)
            override suspend fun register(username_187: String, password_187: String): ApiResult<User> =
                ApiResult.Success(User(id = 7, username = username_187, password = ""))
            override suspend fun login(username_187: String, password_187: String): ApiResult<User> =
                ApiResult.Success(User(id = 7, username = username_187, password = ""))
            override suspend fun getGuests(userId_187: Int, query_187: String): ApiResult<List<Guest>> =
                ApiResult.Success(store_187.filter { it.userId == userId_187 })
            override suspend fun createGuest(userId_187: Int, nama_187: String, alamat_187: String, keperluan_187: String): ApiResult<Guest> {
                val created_187 = Guest(id = 100, userId = userId_187, nama = nama_187, alamat = alamat_187, keperluan = keperluan_187, timestamp = 1_700_000_000_000L)
                store_187.add(created_187)
                return ApiResult.Success(created_187)
            }
            override suspend fun updateGuest(guestId_187: Int, userId_187: Int, nama_187: String, alamat_187: String, keperluan_187: String): ApiResult<Guest> {
                val existing_187 = store_187.first { it.id == guestId_187 && it.userId == userId_187 }
                val updated_187 = existing_187.copy(nama = nama_187, alamat = alamat_187, keperluan = keperluan_187)
                store_187.replaceAll { if (it.id == guestId_187) updated_187 else it }
                return ApiResult.Success(updated_187)
            }
            override suspend fun deleteGuest(guestId_187: Int, userId_187: Int): ApiResult<Boolean> {
                store_187.removeAll { it.id == guestId_187 && it.userId == userId_187 }
                return ApiResult.Success(true)
            }
        }

        val remoteGuestRepo_187 = GuestRepository(fakeGuestDao, fakeApi_187)
        assertNull(remoteGuestRepo_187.syncGuestsFromServer(7))
        assertEquals(1, remoteGuestRepo_187.getAllByUser(7).first().size)
        assertEquals("Siti Rahma", remoteGuestRepo_187.getAllByUser(7).first().first().nama)

        assertNull(remoteGuestRepo_187.insertGuestWithError(7, "Dedi", "Surabaya", "Diskusi"))
        assertEquals(2, remoteGuestRepo_187.getAllByUser(7).first().size)

        assertNull(remoteGuestRepo_187.deleteGuestWithError(100, 7))
        assertEquals(1, remoteGuestRepo_187.getAllByUser(7).first().size)
    }

    private class FakeUserDao : UserDao {
        private val users = mutableListOf<User>()
        private var nextId = 1

        override suspend fun insert(user: User): Long {
            if (users.any { it.username == user.username }) {
                throw android.database.sqlite.SQLiteConstraintException("UNIQUE constraint failed: users.username")
            }
            val saved = user.copy(id = nextId++)
            users.add(saved)
            return saved.id.toLong()
        }

        override suspend fun upsert(user: User): Long {
            users.removeAll { it.id == user.id || it.username == user.username }
            val saved = if (user.id == 0) user.copy(id = nextId++) else user
            users.add(saved)
            return saved.id.toLong()
        }

        override suspend fun findByUsername(username: String): User? {
            return users.firstOrNull { it.username == username }
        }

        override suspend fun delete(user: User) {
            users.removeAll { it.id == user.id }
        }
    }

    private class FakeGuestDao : GuestDao {
        private val guestsFlow = MutableStateFlow<List<Guest>>(emptyList())
        private var nextId = 1

        override suspend fun insert(guest: Guest) {
            val saved = if (guest.id == 0) guest.copy(id = nextId++) else guest
            guestsFlow.value = guestsFlow.value + saved
        }

        override suspend fun upsert(guest: Guest) {
            val saved = if (guest.id == 0) guest.copy(id = nextId++) else guest
            guestsFlow.value = guestsFlow.value.filterNot { it.id == saved.id } + saved
        }

        override suspend fun insertAll(guests: List<Guest>) {
            guests.forEach { upsert(it) }
        }

        override suspend fun update(guest: Guest) {
            guestsFlow.value = guestsFlow.value.map { if (it.id == guest.id) guest else it }
        }

        override suspend fun delete(guest: Guest) {
            guestsFlow.value = guestsFlow.value.filterNot { it.id == guest.id }
        }

        override suspend fun deleteAllByUser(userId: Int) {
            guestsFlow.value = guestsFlow.value.filterNot { it.userId == userId }
        }

        override fun getAllByUser(userId: Int): Flow<List<Guest>> {
            return guestsFlow.map { list ->
                list.filter { it.userId == userId }.sortedByDescending { it.timestamp }
            }
        }

        override fun searchByName(userId: Int, query: String): Flow<List<Guest>> {
            return guestsFlow.map { list ->
                list.filter {
                    it.userId == userId && it.nama.contains(query, ignoreCase = true)
                }.sortedByDescending { it.timestamp }
            }
        }

        override suspend fun getById(id: Int, userId: Int): Guest? {
            return guestsFlow.value.firstOrNull { it.id == id && it.userId == userId }
        }

        override fun countBetween(userId: Int, start: Long, end: Long): Flow<Int> {
            return guestsFlow.map { list ->
                list.count { it.userId == userId && it.timestamp in start..end }
            }
        }

        override fun countAllByUser(userId: Int): Flow<Int> {
            return guestsFlow.map { list ->
                list.count { it.userId == userId }
            }
        }
    }
}

