-- FoodTrack Database Setup for MySQL/MariaDB
-- Run this on your 1blu hosting MySQL database

CREATE DATABASE IF NOT EXISTS foodtrack CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE foodtrack;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Lebensmittel table
CREATE TABLE IF NOT EXISTS lebensmittel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    menge INT DEFAULT 0,
    einheit VARCHAR(100),
    ablaufdatum DATE,
    kategorie VARCHAR(100),
    ean_code VARCHAR(50),
    mindestmenge INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_kategorie (kategorie),
    INDEX idx_ean_code (ean_code)
);

-- Lebensmittel Batches table
CREATE TABLE IF NOT EXISTS lebensmittel_batches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lebensmittel_id INT NOT NULL,
    menge INT NOT NULL,
    ablaufdatum DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lebensmittel_id) REFERENCES lebensmittel(id) ON DELETE CASCADE,
    INDEX idx_lebensmittel_id (lebensmittel_id),
    INDEX idx_ablaufdatum (ablaufdatum)
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lebensmittel_id INT NOT NULL,
    batch_id INT,
    transaction_type ENUM('purchase', 'consumption') NOT NULL,
    menge INT NOT NULL,
    mhd DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lebensmittel_id) REFERENCES lebensmittel(id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES lebensmittel_batches(id) ON DELETE SET NULL,
    INDEX idx_lebensmittel_id (lebensmittel_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_created_at (created_at)
);

-- Insert default admin user (password: admin)
INSERT IGNORE INTO users (email, password_hash) VALUES 
('admin@foodtrack.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');

-- Insert sample data
INSERT IGNORE INTO lebensmittel (name, menge, einheit, kategorie, mindestmenge) VALUES
('Äpfel', 0, 'Stück', 'Obst', 5),
('Milch', 0, 'Liter', 'Milchprodukte', 2),
('Brot', 0, 'Stück', 'Backwaren', 1);
