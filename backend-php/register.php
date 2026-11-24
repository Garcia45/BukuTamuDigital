<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    sendJson_187(405, [
        'success' => false,
        'message' => 'Metode tidak diizinkan. Gunakan POST.',
    ]);
}

$input_187 = getJsonInput_187();
$username_187 = trim((string) ($input_187['username'] ?? ''));
$password_187 = (string) ($input_187['password'] ?? '');

if ($username_187 === '') {
    sendJson_187(400, [
        'success' => false,
        'message' => 'Username wajib diisi',
    ]);
}

if (strlen($password_187) < 6) {
    sendJson_187(400, [
        'success' => false,
        'message' => 'Password minimal 6 karakter',
    ]);
}

$db_187 = getDatabase_187();
$pdo_187 = $db_187['pdo'];

// Pastikan username belum terdaftar pada tabel users_187 (Unique check)
$checkStmt_187 = $pdo_187->prepare('SELECT id FROM users_187 WHERE username = :username_187 LIMIT 1');
$checkStmt_187->execute([':username_187' => $username_187]);
if ($checkStmt_187->fetch()) {
    sendJson_187(409, [
        'success' => false,
        'message' => 'Username sudah digunakan, silakan pilih username lain',
    ]);
}

// Hash password secara aman dengan bcrypt/argon2 bawaan PHP
$hashedPassword_187 = password_hash($password_187, PASSWORD_DEFAULT);

$insertStmt_187 = $pdo_187->prepare(
    'INSERT INTO users_187 (username, password) VALUES (:username_187, :password_187)'
);
$insertStmt_187->execute([
    ':username_187' => $username_187,
    ':password_187' => $hashedPassword_187,
]);

$userId_187 = (int) $pdo_187->lastInsertId();

sendJson_187(201, [
    'success' => true,
    'message' => 'Pendaftaran berhasil',
    'user' => [
        'id' => $userId_187,
        'username' => $username_187,
    ],
]);
