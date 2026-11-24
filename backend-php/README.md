# Backend PHP Simple - Buku Tamu Digital (NIM 187)

Backend REST API sederhana menggunakan **PHP Native + PDO** tanpa framework tambahan, dengan pembeda **3 digit NIM (`187`)** pada seluruh variabel, nama database (`buku_tamu_digital_187` / `buku_tamu_187.sqlite`), dan nama tabel (`users_187`, `guests_187`).

## Fitur API
- `GET /index.php` — Cek status server API, tabel (`users_187`, `guests_187`), & driver database aktif (`mysql` / `sqlite`).
- `POST /register.php` — Registrasi akun petugas ke `users_187` (`username` unik, `password` di-hash `password_hash`).
- `POST /login.php` — Login akun petugas dari `users_187` (`password_verify`).
- `GET /guests.php?user_id={id}&q={opsional}` — Daftar tamu dari `guests_187` khusus milik `user_id` + rekap harian.
- `POST /guests.php` — Tambah tamu baru ke `guests_187` (waktu `timestamp` diisi otomatis oleh server).
- `PUT /guests.php` — Edit data tamu pada `guests_187` (validasi kepemilikan `user_id`, `timestamp` asli tetap utuh).
- `DELETE /guests.php?id={guest_id}&user_id={id}` — Hapus data tamu pada `guests_187` dengan validasi kepemilikan.

## Cara Menjalankan

### Opsi 1 (Paling Cepat - Built-in PHP Server XAMPP)
1. (Opsional) Nyalakan **MySQL** di XAMPP Control Panel jika ingin data masuk ke **MySQL / phpMyAdmin** (database `buku_tamu_digital_187` dan tabel `users_187` / `guests_187` dibuat otomatis, atau impor `database.sql`). Jika MySQL mati, server otomatis fallback ke file SQLite lokal (`buku_tamu_187.sqlite`).
2. Jalankan perintah berikut di terminal:
   ```powershell
   cd "c:\Users\rayhangarcia\Documents\TUGAS\SEM7\Pengembangan Aplikasi Bergerak\BukuTamuDigital\backend-php"
   C:\xampp\php\php.exe -S 0.0.0.0:8000
   ```
3. Buka browser ke `http://localhost:8000/index.php` untuk memastikan server aktif.

### Opsi 2 (Via Apache XAMPP `htdocs`)
1. Salin folder `backend-php` ke `C:\xampp\htdocs\bukutamu-api`.
2. Nyalakan **Apache** dan **MySQL** di XAMPP Control Panel.
3. Ubah `BASE_URL_187` pada `app/src/main/java/com/kelompok3/bukutamu/data/remote/ApiConfig.kt` menjadi:
   - Emulator Android: `http://10.0.2.2/bukutamu-api/`
   - HP Fisik (satu Wi-Fi dengan laptop): `http://<IP-LAPTOP>/bukutamu-api/`

## Pengaturan Alamat Server di Aplikasi Android
Buka file `app/src/main/java/com/kelompok3/bukutamu/data/remote/ApiConfig.kt`:
- **Emulator Android** (pakai Opsi 1 `php -S 0.0.0.0:8000`):
  `const val BASE_URL_187 = "http://10.0.2.2:8000/"` *(default)*
- **HP Fisik via USB Debugging**:
  ```powershell
  adb reverse tcp:8000 tcp:8000
  ```
  Lalu set `BASE_URL_187 = "http://127.0.0.1:8000/"` (atau gunakan IP Wi-Fi laptop).
