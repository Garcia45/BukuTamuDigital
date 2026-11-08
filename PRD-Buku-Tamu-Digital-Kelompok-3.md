# Product Requirements Document (PRD)
## Aplikasi Buku Tamu Digital

**Kelompok:** 3  
**Platform:** Android (Kotlin)  
**Versi dokumen:** 1.0  
**Status:** Draft untuk implementasi

---

## 1. Ringkasan Produk

Buku Tamu Digital adalah aplikasi Android *offline-first* yang menggantikan buku tamu kertas. Setiap pengguna (misalnya resepsionis, satpam, atau staf kantor) dapat mendaftar, masuk, lalu mencatat, melihat, mengubah, dan menghapus data tamu yang berkunjung. Data tersimpan lokal di perangkat menggunakan Room Persistence Library (SQLite), sehingga **tidak memerlukan koneksi internet maupun API/PHP**.

Setiap data tamu bersifat privat: pengguna hanya dapat melihat data tamu miliknya sendiri melalui relasi `user_id` (foreign key).

## 2. Latar Belakang dan Masalah

| Masalah pada buku tamu manual | Solusi pada aplikasi |
|---|---|
| Tulisan tangan sulit dibaca | Input terstruktur lewat form digital |
| Waktu kunjungan sering tidak dicatat | Timestamp otomatis dengan `System.currentTimeMillis()` |
| Sulit merekap jumlah tamu | Rekapitulasi jumlah tamu harian otomatis |
| Data mudah hilang atau dilihat orang lain | Penyimpanan Room + isolasi data per user |
| Sulit mencari atau mengoreksi data | List, edit, dan hapus data |

## 3. Tujuan

### 3.1 Tujuan Produk
1. Menyediakan pencatatan tamu yang cepat, rapi, dan dapat diandalkan tanpa internet.
2. Menjamin privasi: data satu user tidak dapat diakses user lain.
3. Menampilkan rekap jumlah tamu harian pada dashboard.

### 3.2 Tujuan Akademik (Tugas Kelompok)
1. Mengimplementasikan Room Database (Entity, DAO, Database) dengan relasi foreign key.
2. Mengimplementasikan autentikasi lokal (register, login, sesi, logout).
3. Mengimplementasikan CRUD lengkap dengan RecyclerView.
4. Menerapkan alur navigasi multi-layar yang jelas.

### 3.3 Metrik Keberhasilan
- Semua fitur CRUD berjalan tanpa *crash* pada skenario uji.
- User A tidak pernah melihat data User B (0 kebocoran data pada pengujian).
- Waktu tamu tercatat otomatis pada 100% data baru.
- Angka rekap harian sesuai dengan jumlah data pada list untuk hari yang sama.

## 4. Ruang Lingkup

### 4.1 Dalam Lingkup (In Scope)
- Register, Login, Sesi (SharedPreferences), Logout.
- Dashboard dengan rekap jumlah tamu hari ini.
- List tamu (RecyclerView), tambah, edit, hapus tamu.
- Pencatatan waktu otomatis.
- Isolasi data per user.
- Penyimpanan lokal dengan Room.

### 4.2 Di Luar Lingkup (Out of Scope)
- Sinkronisasi cloud, API, server PHP, atau koneksi internet.
- Ekspor PDF/Excel, cetak, atau berbagi data.
- Foto/tanda tangan tamu, QR code, notifikasi.
- Peran admin, multi-perangkat, pemulihan password lewat email.

*(Item di atas dapat dijadikan pengembangan lanjutan.)*

## 5. Target Pengguna

| Persona | Deskripsi | Kebutuhan Utama |
|---|---|---|
| Petugas penerima tamu | Resepsionis/satpam yang mencatat tamu setiap hari | Input cepat, melihat daftar, koreksi data |
| Pemilik akun lain di perangkat yang sama | User berbeda memakai aplikasi yang sama | Data pribadi tidak tercampur |

## 6. Tech Stack

| Komponen | Pilihan |
|---|---|
| IDE | Android Studio |
| Bahasa | Kotlin |
| Database | Room Persistence Library (SQLite) |
| Sesi | SharedPreferences |
| UI list | RecyclerView + Adapter (ViewHolder) |
| Asinkron | Kotlin Coroutines + `suspend` DAO / `Flow` |
| Arsitektur | MVVM sederhana (Activity/Fragment, ViewModel, Repository, DAO) |
| Min SDK | 24 (Android 7.0) atau sesuai kesepakatan kelompok |

**Dependensi utama (Gradle, ringkas):** `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (via KSP/kapt), `lifecycle-viewmodel-ktx`, `lifecycle-livedata-ktx`, `kotlinx-coroutines-android`, `recyclerview`, `material`.

## 7. Struktur Data

### 7.1 Entity: User

| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | Int (PK, autoGenerate) | Identitas unik user |
| `username` | String | Unik (unique index), wajib |
| `password` | String | Disimpan dalam bentuk hash (lihat bagian 9) |

### 7.2 Entity: Guest

| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | Int (PK, autoGenerate) | Identitas unik tamu |
| `user_id` | Int (FK → User.id) | Pemilik data; `onDelete = CASCADE`; di-index |
| `nama` | String | Wajib |
| `alamat` | String | Wajib |
| `keperluan` | String | Wajib |
| `timestamp` | Long | Diisi `System.currentTimeMillis()` saat data dibuat |

### 7.3 Relasi

`User (1) ──── (N) Guest`. Satu user memiliki banyak tamu; setiap tamu hanya dimiliki satu user.

### 7.4 Contoh Kode Entity

```kotlin
@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String
)

@Entity(
    tableName = "guests",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
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
```

### 7.5 Kebutuhan Query (DAO)

```kotlin
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?
}

@Dao
interface GuestDao {
    @Insert suspend fun insert(guest: Guest)
    @Update suspend fun update(guest: Guest)
    @Delete suspend fun delete(guest: Guest)

    @Query("SELECT * FROM guests WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllByUser(userId: Int): Flow<List<Guest>>

    @Query("SELECT * FROM guests WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getById(id: Int, userId: Int): Guest?

    @Query("SELECT COUNT(*) FROM guests WHERE user_id = :userId AND timestamp BETWEEN :start AND :end")
    fun countBetween(userId: Int, start: Long, end: Long): Flow<Int>
}
```

> **Aturan penting:** semua query `Guest` **wajib** menyertakan filter `user_id`. Ini adalah inti dari isolasi data.

## 8. Kebutuhan Fungsional

### 8.1 Autentikasi

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-01 | User dapat mendaftar dengan username dan password. | Wajib |
| FR-02 | Username harus unik; jika sudah dipakai tampilkan pesan error. | Wajib |
| FR-03 | Validasi register: semua field wajib diisi, password minimal 6 karakter, dan konfirmasi password harus sama. | Wajib |
| FR-04 | User dapat login dengan username dan password yang benar. | Wajib |
| FR-05 | Jika kredensial salah, tampilkan pesan "Username atau password salah". | Wajib |
| FR-06 | Setelah login, simpan sesi (`user_id`, `username`, status login) di SharedPreferences. | Wajib |
| FR-07 | Saat aplikasi dibuka, jika sesi aktif langsung ke Dashboard; jika tidak, ke Login. | Wajib |
| FR-08 | Logout menghapus sesi dan kembali ke Login tanpa bisa kembali dengan tombol Back (clear back stack). | Wajib |

### 8.2 Dashboard

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-09 | Menampilkan salam dengan username yang sedang login. | Wajib |
| FR-10 | Menampilkan **rekap jumlah tamu hari ini** (dari 00:00:00 sampai 23:59:59 waktu perangkat). | Wajib |
| FR-11 | Menyediakan tombol menuju List Tamu dan Tambah Tamu. | Wajib |
| FR-12 | Menyediakan tombol/menu Logout. | Wajib |
| FR-13 | Menampilkan total keseluruhan tamu milik user. | Opsional |

### 8.3 CRUD Tamu

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-14 | **Read:** List tamu ditampilkan dengan RecyclerView, diurutkan dari yang terbaru. | Wajib |
| FR-15 | Setiap item menampilkan nama, keperluan, dan waktu kunjungan terformat (contoh: `08 Nov 2026, 14:30`). | Wajib |
| FR-16 | List hanya berisi tamu dengan `user_id` sama dengan user yang login. | Wajib |
| FR-17 | Tampilkan *empty state* jika belum ada data. | Wajib |
| FR-18 | **Create:** Form tambah dengan field nama, alamat, keperluan; semua wajib diisi. | Wajib |
| FR-19 | Saat simpan, `timestamp` diisi otomatis dengan `System.currentTimeMillis()` dan `user_id` diambil dari sesi; user tidak mengisi keduanya. | Wajib |
| FR-20 | **Update:** Mengetuk item membuka form edit yang terisi data lama; timestamp asli **tidak berubah**. | Wajib |
| FR-21 | **Delete:** Hapus dengan dialog konfirmasi ("Yakin ingin menghapus?"). | Wajib |
| FR-22 | Setelah tambah/edit/hapus, list dan rekap diperbarui otomatis. | Wajib |
| FR-23 | Pencarian tamu berdasarkan nama. | Opsional |

### 8.4 Rekapitulasi Harian

| ID | Kebutuhan | Prioritas |
|---|---|---|
| FR-24 | Hitung jumlah tamu per hari memakai rentang timestamp awal dan akhir hari. | Wajib |
| FR-25 | Rekap hanya menghitung data milik user yang login. | Wajib |
| FR-26 | Rekap harian dapat tampil sebagai daftar tanggal beserta jumlah tamu (riwayat). | Opsional |

**Cara menghitung rentang hari (Kotlin):**

```kotlin
val cal = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}
val startOfDay = cal.timeInMillis
val endOfDay = startOfDay + 24L * 60 * 60 * 1000 - 1
```

## 9. Kebutuhan Non-Fungsional

| Kategori | Kebutuhan |
|---|---|
| Offline | Seluruh fitur berjalan tanpa internet; tidak ada permission `INTERNET`. |
| Keamanan | Password **tidak disimpan sebagai teks biasa**; gunakan hash (misalnya SHA-256 dengan salt) sebelum disimpan. Validasi kepemilikan data (`user_id`) pada setiap operasi ubah/hapus. |
| Performa | Operasi database dijalankan di luar main thread (Coroutines); list tetap lancar untuk minimal 1.000 data. |
| Usability | Form dengan validasi jelas, pesan error informatif, tombol berukuran nyaman disentuh. |
| Kompatibilitas | Berjalan pada Android 7.0 (API 24) ke atas. |
| Ketahanan | Data tidak hilang saat rotasi layar atau aplikasi ditutup. |
| Maintainability | Kode terstruktur per package (data, ui, util), mengikuti konvensi Kotlin. |

## 10. Alur Navigasi

```
[Splash/Cek Sesi]
      │
      ├── belum login ──► [Login] ◄──► [Register]
      │                      │
      └── sudah login ───────┤
                             ▼
                        [Dashboard]
                             │
                  ┌──────────┴──────────┐
                  ▼                     ▼
          [List Tamu]  ──────────► [Form Tambah]
          (RecyclerView)
                  │
                  ▼
          [Form Edit / Hapus]

[Dashboard] ── Logout ──► [Login]
```

Alur utama: **Login → Dashboard → List Data (RecyclerView) → Form Tambah → Edit/Hapus.**

## 11. Spesifikasi Layar

### 11.1 Layar Login
- Elemen: input username, input password, tombol **Login**, teks tautan "Belum punya akun? Daftar".
- Validasi: field tidak boleh kosong.
- Hasil sukses: simpan sesi lalu buka Dashboard.

### 11.2 Layar Register
- Elemen: username, password, konfirmasi password, tombol **Daftar**, tautan kembali ke Login.
- Hasil sukses: tampilkan pesan berhasil lalu kembali ke Login.

### 11.3 Layar Dashboard
- Elemen: salam pengguna, kartu **Tamu Hari Ini** (angka), tombol **Lihat Daftar Tamu**, tombol **Tambah Tamu**, tombol **Logout**.

### 11.4 Layar List Tamu
- Elemen: RecyclerView, FAB (+) untuk tambah tamu, empty state.
- Item: nama, keperluan, waktu kunjungan.
- Interaksi: ketuk item membuka Form Edit; tombol hapus/ikon sampah atau tekan lama memunculkan dialog konfirmasi hapus.

### 11.5 Layar Form Tambah / Edit
- Elemen: input nama, alamat, keperluan, tombol **Simpan**, tombol Batal.
- Mode tambah: field kosong. Mode edit: field terisi data tamu terpilih.
- Waktu kunjungan tidak dapat diubah pada form (ditampilkan sebagai teks saja pada mode edit).
- Satu layar (Activity/Fragment) dapat dipakai ulang untuk tambah dan edit.

## 12. Aturan Bisnis

1. Username bersifat unik dan tidak peka terhadap spasi di awal/akhir (lakukan `trim()`).
2. `user_id` pada Guest selalu berasal dari sesi, tidak pernah dari input user.
3. `timestamp` hanya diisi sekali saat data dibuat dan tidak berubah saat edit.
4. Edit dan hapus hanya diizinkan untuk data dengan `user_id` yang sama dengan sesi.
5. Hari dihitung berdasarkan zona waktu perangkat.
6. Jika user dihapus, seluruh tamu miliknya ikut terhapus (CASCADE).

## 13. Struktur Proyek yang Disarankan

```
com.kelompok3.bukutamu
├── data
│   ├── AppDatabase.kt
│   ├── entity   (User.kt, Guest.kt)
│   ├── dao      (UserDao.kt, GuestDao.kt)
│   └── repository (AuthRepository.kt, GuestRepository.kt)
├── ui
│   ├── auth     (LoginActivity, RegisterActivity)
│   ├── dashboard (DashboardActivity)
│   ├── guest    (GuestListActivity, GuestAdapter, GuestFormActivity)
│   └── viewmodel (AuthViewModel, GuestViewModel)
└── util
    ├── SessionManager.kt   // SharedPreferences
    ├── PasswordHasher.kt
    └── DateUtils.kt        // format tanggal & rentang hari
```

**SessionManager (kunci SharedPreferences):** `is_logged_in` (Boolean), `user_id` (Int), `username` (String).

## 14. Kriteria Penerimaan (Acceptance Criteria)

| ID | Skenario | Hasil yang Diharapkan |
|---|---|---|
| AC-01 | Register dengan username baru | Akun dibuat, diarahkan ke Login |
| AC-02 | Register dengan username yang sudah ada | Muncul pesan error, akun tidak dibuat |
| AC-03 | Login dengan kredensial benar | Masuk ke Dashboard, sesi tersimpan |
| AC-04 | Login dengan password salah | Muncul pesan error, tetap di Login |
| AC-05 | Tutup lalu buka ulang aplikasi saat sudah login | Langsung ke Dashboard |
| AC-06 | Logout lalu tekan tombol Back | Tidak bisa kembali ke Dashboard |
| AC-07 | Tambah tamu dengan semua field terisi | Data muncul di list dengan waktu otomatis |
| AC-08 | Tambah tamu dengan field kosong | Muncul validasi, data tidak tersimpan |
| AC-09 | Edit data tamu | Perubahan tersimpan, timestamp tetap |
| AC-10 | Hapus tamu dan konfirmasi | Data hilang dari list, rekap berkurang |
| AC-11 | Login sebagai User B setelah User A menambah data | User B tidak melihat data User A |
| AC-12 | Tambah 3 tamu hari ini | Rekap Dashboard menunjukkan angka 3 |
| AC-13 | Tamu bertanggal kemarin | Tidak dihitung pada rekap hari ini |
| AC-14 | Rotasi layar saat mengisi form | Input tidak hilang (atau perilaku terdefinisi) |

## 15. Rencana Pengujian

- **Unit test:** `PasswordHasher`, fungsi rentang hari, validasi input.
- **Instrumented test (Room):** insert, update, delete, query dengan filter `user_id`, pengujian CASCADE, pengujian unique username.
- **UI/Manual test:** seluruh skenario pada bagian 14 dengan minimal dua akun berbeda.
- **Uji tepi:** tamu dibuat tepat sekitar tengah malam, data berjumlah banyak, input sangat panjang atau berisi karakter khusus.

## 16. Rencana Pengerjaan (Saran Pembagian Tugas)

| Tahap | Pekerjaan | Keluaran |
|---|---|---|
| 1 | Setup proyek, dependensi, struktur package | Proyek siap |
| 2 | Entity, DAO, AppDatabase, Repository | Lapisan data teruji |
| 3 | SessionManager, Register, Login, Logout | Autentikasi berjalan |
| 4 | Dashboard dan rekap harian | Dashboard fungsional |
| 5 | List (RecyclerView), Form Tambah, Edit, Hapus | CRUD lengkap |
| 6 | Validasi, UI polishing, pengujian | Aplikasi stabil |
| 7 | Dokumentasi dan presentasi | Laporan dan demo |

## 17. Risiko dan Mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Lupa filter `user_id` pada query | Kebocoran data antar user | Semua query lewat Repository yang mewajibkan `userId`; uji dengan dua akun |
| Password tersimpan polos | Masalah keamanan | Gunakan hash sebelum disimpan |
| Query database di main thread | Aplikasi *freeze*/crash | Gunakan Coroutines dan `suspend`/`Flow` |
| Salah hitung rentang hari | Rekap tidak akurat | Satu fungsi util yang diuji unit test |
| Perubahan skema Room | Aplikasi crash saat update | Naikkan versi database dan sediakan migrasi atau gunakan `fallbackToDestructiveMigration()` selama pengembangan |
| Sesi tertinggal setelah logout | Akses tidak sah | Hapus seluruh preferensi sesi dan bersihkan back stack |

## 18. Pengembangan Lanjutan (Backlog)

- Ekspor rekap ke PDF atau CSV.
- Pencarian dan filter tanggal.
- Grafik kunjungan mingguan/bulanan.
- Foto tamu atau tanda tangan digital.
- Ubah password dan hapus akun.
- Backup/restore database.

## 19. Lampiran: Glosarium

| Istilah | Arti |
|---|---|
| Room | Library Android sebagai lapisan abstraksi di atas SQLite |
| Entity | Kelas yang merepresentasikan tabel |
| DAO | Data Access Object, antarmuka berisi query |
| Foreign key | Kolom yang merujuk ke primary key tabel lain |
| Sesi | Status login yang tersimpan di SharedPreferences |
| Isolasi data | Pembatasan agar user hanya mengakses datanya sendiri |
