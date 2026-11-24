-- Skema Database MySQL untuk Backend PHP Buku Tamu Digital (NIM 187)
-- Impor file ini melalui phpMyAdmin atau biarkan config.php membuatnya otomatis.

CREATE DATABASE IF NOT EXISTS `buku_tamu_digital_187`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `buku_tamu_digital_187`;

CREATE TABLE IF NOT EXISTS `users_187` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(100) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `guests_187` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `nama` VARCHAR(150) NOT NULL,
  `alamat` TEXT NOT NULL,
  `keperluan` TEXT NOT NULL,
  `timestamp` BIGINT NOT NULL,
  INDEX `idx_guests_187_user_id` (`user_id`),
  CONSTRAINT `fk_guests_187_user`
    FOREIGN KEY (`user_id`) REFERENCES `users_187`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
