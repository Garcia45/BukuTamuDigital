<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

$db_187 = getDatabase_187();

sendJson_187(200, [
    'success' => true,
    'app' => 'Buku Tamu Digital PHP REST API (NIM 187)',
    'database_driver' => $db_187['driver'],
    'tables' => ['users_187', 'guests_187'],
    'endpoints' => [
        'POST /register.php' => 'Daftar akun petugas baru {username, password}',
        'POST /login.php' => 'Masuk akun petugas {username, password}',
        'GET /guests.php?user_id={id}&q={opsional}' => 'Ambil daftar tamu milik user_id',
        'GET /guests.php?user_id={id}&id={guest_id}' => 'Ambil detail 1 tamu milik user_id',
        'POST /guests.php' => 'Tambah tamu baru {user_id, nama, alamat, keperluan}',
        'PUT /guests.php' => 'Edit tamu milik user_id {id, user_id, nama, alamat, keperluan}',
        'DELETE /guests.php?id={guest_id}&user_id={id}' => 'Hapus tamu milik user_id',
    ],
]);
