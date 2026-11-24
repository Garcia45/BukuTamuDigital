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

if ($username_187 === '' || $password_187 === '') {
    sendJson_187(400, [
        'success' => false,
        'message' => 'Username dan password wajib diisi',
    ]);
}

$db_187 = getDatabase_187();
$pdo_187 = $db_187['pdo'];

$stmt_187 = $pdo_187->prepare(
    'SELECT id, username, password FROM users_187 WHERE username = :username_187 LIMIT 1'
);
$stmt_187->execute([':username_187' => $username_187]);
$user_187 = $stmt_187->fetch();

if (!$user_187 || !password_verify($password_187, (string) $user_187['password'])) {
    sendJson_187(401, [
        'success' => false,
        'message' => 'Username atau password salah',
    ]);
}

sendJson_187(200, [
    'success' => true,
    'message' => 'Login berhasil',
    'user' => [
        'id' => (int) $user_187['id'],
        'username' => (string) $user_187['username'],
    ],
]);
