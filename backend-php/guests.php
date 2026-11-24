<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

$method_187 = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$db_187 = getDatabase_187();
$pdo_187 = $db_187['pdo'];

// Format standar record Guest dari tabel guests_187 untuk dikirim ke aplikasi Android
function formatGuestRow_187(array $row_187): array
{
    return [
        'id' => (int) $row_187['id'],
        'user_id' => (int) $row_187['user_id'],
        'nama' => (string) $row_187['nama'],
        'alamat' => (string) $row_187['alamat'],
        'keperluan' => (string) $row_187['keperluan'],
        'timestamp' => (int) $row_187['timestamp'],
    ];
}

// Memastikan user_id valid dan terdaftar di tabel users_187
function requireValidUser_187(PDO $pdo_187, int $userId_187): void
{
    if ($userId_187 <= 0) {
        sendJson_187(400, [
            'success' => false,
            'message' => 'Parameter user_id tidak valid',
        ]);
    }

    $stmt_187 = $pdo_187->prepare('SELECT id FROM users_187 WHERE id = :id_187 LIMIT 1');
    $stmt_187->execute([':id_187' => $userId_187]);
    if (!$stmt_187->fetch()) {
        sendJson_187(404, [
            'success' => false,
            'message' => 'Akun pengguna tidak ditemukan',
        ]);
    }
}

// ==========================================================
// GET /guests.php?user_id=1[&q=nama][&id=guestId]
// ==========================================================
if ($method_187 === 'GET') {
    $userId_187 = (int) ($_GET['user_id'] ?? 0);
    requireValidUser_187($pdo_187, $userId_187);

    $guestId_187 = isset($_GET['id']) ? (int) $_GET['id'] : 0;
    if ($guestId_187 > 0) {
        $stmt_187 = $pdo_187->prepare(
            'SELECT * FROM guests_187 WHERE id = :id_187 AND user_id = :user_id_187 LIMIT 1'
        );
        $stmt_187->execute([
            ':id_187' => $guestId_187,
            ':user_id_187' => $userId_187,
        ]);
        $row_187 = $stmt_187->fetch();
        if (!$row_187) {
            sendJson_187(404, [
                'success' => false,
                'message' => 'Data tamu tidak ditemukan atau bukan milik akun ini',
            ]);
        }

        sendJson_187(200, [
            'success' => true,
            'guest' => formatGuestRow_187($row_187),
        ]);
    }

    $query_187 = trim((string) ($_GET['q'] ?? ''));
    if ($query_187 !== '') {
        $stmt_187 = $pdo_187->prepare('
            SELECT * FROM guests_187
            WHERE user_id = :user_id_187 AND nama LIKE :q_187
            ORDER BY timestamp DESC
        ');
        $stmt_187->execute([
            ':user_id_187' => $userId_187,
            ':q_187' => '%' . $query_187 . '%',
        ]);
    } else {
        $stmt_187 = $pdo_187->prepare('
            SELECT * FROM guests_187
            WHERE user_id = :user_id_187
            ORDER BY timestamp DESC
        ');
        $stmt_187->execute([':user_id_187' => $userId_187]);
    }

    $rows_187 = $stmt_187->fetchAll();
    $guests_187 = array_map('formatGuestRow_187', $rows_187);

    // Hitung rekap hari ini (00:00:00.000 s.d. 23:59:59.999) khusus user_id pada guests_187
    $startOfDayMs_187 = strtotime('today 00:00:00') * 1000;
    $endOfDayMs_187 = (strtotime('tomorrow 00:00:00') * 1000) - 1;

    $todayStmt_187 = $pdo_187->prepare('
        SELECT COUNT(*) AS cnt FROM guests_187
        WHERE user_id = :user_id_187 AND timestamp BETWEEN :start_187 AND :end_187
    ');
    $todayStmt_187->execute([
        ':user_id_187' => $userId_187,
        ':start_187' => $startOfDayMs_187,
        ':end_187' => $endOfDayMs_187,
    ]);
    $todayCount_187 = (int) ($todayStmt_187->fetch()['cnt'] ?? 0);

    $totalStmt_187 = $pdo_187->prepare('SELECT COUNT(*) AS cnt FROM guests_187 WHERE user_id = :user_id_187');
    $totalStmt_187->execute([':user_id_187' => $userId_187]);
    $totalCount_187 = (int) ($totalStmt_187->fetch()['cnt'] ?? 0);

    sendJson_187(200, [
        'success' => true,
        'today_count' => $todayCount_187,
        'total_count' => $totalCount_187,
        'guests' => $guests_187,
    ]);
}

// ==========================================================
// POST /guests.php (Tambah Tamu Baru ke guests_187)
// ==========================================================
if ($method_187 === 'POST') {
    $input_187 = getJsonInput_187();
    $action_187 = strtolower((string) ($input_187['action'] ?? 'create'));
    if ($action_187 === 'update') {
        $method_187 = 'PUT';
    } elseif ($action_187 === 'delete') {
        $method_187 = 'DELETE';
    } else {
        $userId_187 = (int) ($input_187['user_id'] ?? 0);
        requireValidUser_187($pdo_187, $userId_187);

        $nama_187 = trim((string) ($input_187['nama'] ?? ''));
        $alamat_187 = trim((string) ($input_187['alamat'] ?? ''));
        $keperluan_187 = trim((string) ($input_187['keperluan'] ?? ''));

        if ($nama_187 === '' || $alamat_187 === '' || $keperluan_187 === '') {
            sendJson_187(400, [
                'success' => false,
                'message' => 'Nama, alamat, dan keperluan wajib diisi',
            ]);
        }

        // Timestamp diisi otomatis oleh server dalam milidetik
        $timestamp_187 = (int) round(microtime(true) * 1000);

        $stmt_187 = $pdo_187->prepare('
            INSERT INTO guests_187 (user_id, nama, alamat, keperluan, timestamp)
            VALUES (:user_id_187, :nama_187, :alamat_187, :keperluan_187, :timestamp_187)
        ');
        $stmt_187->execute([
            ':user_id_187' => $userId_187,
            ':nama_187' => $nama_187,
            ':alamat_187' => $alamat_187,
            ':keperluan_187' => $keperluan_187,
            ':timestamp_187' => $timestamp_187,
        ]);

        $newId_187 = (int) $pdo_187->lastInsertId();
        sendJson_187(201, [
            'success' => true,
            'message' => 'Data tamu berhasil disimpan',
            'guest' => [
                'id' => $newId_187,
                'user_id' => $userId_187,
                'nama' => $nama_187,
                'alamat' => $alamat_187,
                'keperluan' => $keperluan_187,
                'timestamp' => $timestamp_187,
            ],
        ]);
    }
}

// ==========================================================
// PUT /guests.php (Edit Tamu di guests_187 - Timestamp Asli Dipertahankan)
// ==========================================================
if ($method_187 === 'PUT') {
    $input_187 = getJsonInput_187();
    $guestId_187 = (int) ($input_187['id'] ?? $_GET['id'] ?? 0);
    $userId_187 = (int) ($input_187['user_id'] ?? $_GET['user_id'] ?? 0);
    requireValidUser_187($pdo_187, $userId_187);

    $nama_187 = trim((string) ($input_187['nama'] ?? ''));
    $alamat_187 = trim((string) ($input_187['alamat'] ?? ''));
    $keperluan_187 = trim((string) ($input_187['keperluan'] ?? ''));

    if ($guestId_187 <= 0 || $nama_187 === '' || $alamat_187 === '' || $keperluan_187 === '') {
        sendJson_187(400, [
            'success' => false,
            'message' => 'ID tamu, nama, alamat, dan keperluan wajib diisi',
        ]);
    }

    // Validasi kepemilikan data sebelum update
    $checkStmt_187 = $pdo_187->prepare(
        'SELECT * FROM guests_187 WHERE id = :id_187 AND user_id = :user_id_187 LIMIT 1'
    );
    $checkStmt_187->execute([
        ':id_187' => $guestId_187,
        ':user_id_187' => $userId_187,
    ]);
    $existing_187 = $checkStmt_187->fetch();

    if (!$existing_187) {
        sendJson_187(403, [
            'success' => false,
            'message' => 'Akses ditolak: Data tamu tidak ditemukan atau bukan milik akun ini',
        ]);
    }

    // Timestamp asli dipertahankan dan tidak diubah saat edit
    $updateStmt_187 = $pdo_187->prepare('
        UPDATE guests_187
        SET nama = :nama_187, alamat = :alamat_187, keperluan = :keperluan_187
        WHERE id = :id_187 AND user_id = :user_id_187
    ');
    $updateStmt_187->execute([
        ':nama_187' => $nama_187,
        ':alamat_187' => $alamat_187,
        ':keperluan_187' => $keperluan_187,
        ':id_187' => $guestId_187,
        ':user_id_187' => $userId_187,
    ]);

    sendJson_187(200, [
        'success' => true,
        'message' => 'Data tamu berhasil diperbarui',
        'guest' => [
            'id' => $guestId_187,
            'user_id' => $userId_187,
            'nama' => $nama_187,
            'alamat' => $alamat_187,
            'keperluan' => $keperluan_187,
            'timestamp' => (int) $existing_187['timestamp'],
        ],
    ]);
}

// ==========================================================
// DELETE /guests.php?id=5&user_id=1
// ==========================================================
if ($method_187 === 'DELETE') {
    $input_187 = getJsonInput_187();
    $guestId_187 = (int) ($_GET['id'] ?? $input_187['id'] ?? 0);
    $userId_187 = (int) ($_GET['user_id'] ?? $input_187['user_id'] ?? 0);
    requireValidUser_187($pdo_187, $userId_187);

    if ($guestId_187 <= 0) {
        sendJson_187(400, [
            'success' => false,
            'message' => 'ID tamu tidak valid',
        ]);
    }

    $stmt_187 = $pdo_187->prepare(
        'DELETE FROM guests_187 WHERE id = :id_187 AND user_id = :user_id_187'
    );
    $stmt_187->execute([
        ':id_187' => $guestId_187,
        ':user_id_187' => $userId_187,
    ]);

    if ($stmt_187->rowCount() === 0) {
        sendJson_187(403, [
            'success' => false,
            'message' => 'Akses ditolak: Data tamu tidak ditemukan atau bukan milik akun ini',
        ]);
    }

    sendJson_187(200, [
        'success' => true,
        'message' => 'Data tamu berhasil dihapus',
    ]);
}

sendJson_187(405, [
    'success' => false,
    'message' => 'Metode HTTP tidak didukung',
]);
