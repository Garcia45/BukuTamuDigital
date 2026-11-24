<?php
declare(strict_types=1);

// Header standar REST API JSON & CORS (NIM 187)
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(200);
    exit;
}

date_default_timezone_set('Asia/Jakarta');

/**
 * Konfigurasi Database (NIM 187):
 * - 'auto'   : Mencoba MySQL (XAMPP) terlebih dahulu; jika MySQL belum aktif, otomatis fallback ke SQLite lokal.
 * - 'mysql'  : Wajib memakai MySQL (XAMPP / MariaDB).
 * - 'sqlite' : Langsung memakai file SQLite lokal (buku_tamu_187.sqlite).
 */
const DB_DRIVER_187   = 'auto';
const MYSQL_HOST_187  = '127.0.0.1';
const MYSQL_PORT_187  = 3306;
const MYSQL_DB_187    = 'buku_tamu_digital_187';
const MYSQL_USER_187  = 'root';
const MYSQL_PASS_187  = '';
const SQLITE_PATH_187 = __DIR__ . DIRECTORY_SEPARATOR . 'buku_tamu_187.sqlite';

/**
 * Mengirim respon JSON dengan HTTP status code yang sesuai dan menghentikan eksekusi.
 */
function sendJson_187(int $statusCode_187, array $payload_187): void
{
    http_response_code($statusCode_187);
    echo json_encode($payload_187, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

/**
 * Membaca payload JSON dari request body (fallback ke $_POST jika form-encoded).
 */
function getJsonInput_187(): array
{
    $raw_187 = file_get_contents('php://input');
    if ($raw_187 !== false && trim($raw_187) !== '') {
        $decoded_187 = json_decode($raw_187, true);
        if (is_array($decoded_187)) {
            return $decoded_187;
        }
    }
    return is_array($_POST) ? $_POST : [];
}

/**
 * Inisialisasi koneksi PDO dan memastikan tabel users_187 & guests_187 tersedia.
 *
 * @return array{pdo: PDO, driver: string}
 */
function getDatabase_187(): array
{
    static $cached_187 = null;
    if ($cached_187 !== null) {
        return $cached_187;
    }

    $driverMode_187 = getenv('BUKU_TAMU_DB_DRIVER_187') ?: DB_DRIVER_187;

    if ($driverMode_187 === 'mysql' || $driverMode_187 === 'auto') {
        try {
            $pdo_187 = connectMySql_187();
            $cached_187 = ['pdo' => $pdo_187, 'driver' => 'mysql'];
            return $cached_187;
        } catch (Throwable $e_187) {
            if ($driverMode_187 === 'mysql') {
                sendJson_187(500, [
                    'success' => false,
                    'message' => 'Gagal terhubung ke database MySQL: ' . $e_187->getMessage(),
                ]);
            }
            // Mode 'auto': fallback ke SQLite lokal jika service MySQL sedang mati
        }
    }

    try {
        $pdo_187 = connectSqlite_187();
        $cached_187 = ['pdo' => $pdo_187, 'driver' => 'sqlite'];
        return $cached_187;
    } catch (Throwable $e_187) {
        sendJson_187(500, [
            'success' => false,
            'message' => 'Gagal menginisialisasi database SQLite: ' . $e_187->getMessage(),
        ]);
    }
}

function connectMySql_187(): PDO
{
    $dsnServer_187 = sprintf('mysql:host=%s;port=%d;charset=utf8mb4', MYSQL_HOST_187, MYSQL_PORT_187);
    $options_187 = [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_TIMEOUT => 2,
    ];

    $serverPdo_187 = new PDO($dsnServer_187, MYSQL_USER_187, MYSQL_PASS_187, $options_187);
    $serverPdo_187->exec(sprintf(
        'CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        MYSQL_DB_187
    ));

    $dsnDb_187 = sprintf(
        'mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4',
        MYSQL_HOST_187,
        MYSQL_PORT_187,
        MYSQL_DB_187
    );
    $pdo_187 = new PDO($dsnDb_187, MYSQL_USER_187, MYSQL_PASS_187, $options_187);

    $pdo_187->exec('
        CREATE TABLE IF NOT EXISTS users_187 (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    ');

    $pdo_187->exec('
        CREATE TABLE IF NOT EXISTS guests_187 (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            nama VARCHAR(150) NOT NULL,
            alamat TEXT NOT NULL,
            keperluan TEXT NOT NULL,
            timestamp BIGINT NOT NULL,
            INDEX idx_guests_187_user_id (user_id),
            CONSTRAINT fk_guests_187_user
                FOREIGN KEY (user_id) REFERENCES users_187(id)
                ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    ');

    return $pdo_187;
}

function connectSqlite_187(): PDO
{
    $sqlitePath_187 = getenv('BUKU_TAMU_SQLITE_PATH_187') ?: SQLITE_PATH_187;
    $pdo_187 = new PDO('sqlite:' . $sqlitePath_187, null, null, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $pdo_187->exec('PRAGMA foreign_keys = ON');

    $pdo_187->exec('
        CREATE TABLE IF NOT EXISTS users_187 (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ');

    $pdo_187->exec('
        CREATE TABLE IF NOT EXISTS guests_187 (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            nama TEXT NOT NULL,
            alamat TEXT NOT NULL,
            keperluan TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users_187(id) ON DELETE CASCADE
        )
    ');
    $pdo_187->exec('CREATE INDEX IF NOT EXISTS idx_guests_187_user_id ON guests_187(user_id)');

    return $pdo_187;
}
