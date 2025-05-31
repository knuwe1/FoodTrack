-- FoodTrack Database Setup for MySQL/MariaDB
-- Run this on your 1blu hosting MySQL database

CREATE DATABASE IF NOT EXISTS foodtrack CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE foodtrack;

-- Households table (Multi-Tenant)
CREATE TABLE IF NOT EXISTS households (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    admin_user_id INT,
    invite_code VARCHAR(50) UNIQUE,
    invite_expires_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invite_code (invite_code),
    INDEX idx_admin_user (admin_user_id)
);

-- Users table (Extended for Multi-Tenant)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    household_id INT NOT NULL,
    role ENUM('admin', 'member') DEFAULT 'member',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    INDEX idx_household_id (household_id),
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- Add foreign key constraint for household admin
ALTER TABLE households ADD CONSTRAINT fk_household_admin
    FOREIGN KEY (admin_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Storage Locations table (Household-specific)
CREATE TABLE IF NOT EXISTS storage_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    household_id INT NOT NULL,
    location_type ENUM('pantry', 'refrigerator', 'freezer', 'cellar', 'garage', 'other') DEFAULT 'pantry',
    temperature_zone ENUM('frozen', 'refrigerated', 'cool', 'room_temperature', 'warm') DEFAULT 'room_temperature',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    INDEX idx_household_id (household_id),
    INDEX idx_location_type (location_type),
    INDEX idx_temperature_zone (temperature_zone)
);

-- Packages table (Global - can be used by all households)
CREATE TABLE IF NOT EXISTS packages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    package_type ENUM('piece', 'pack', 'bottle', 'can', 'jar', 'bag', 'box', 'tube', 'container', 'bulk') DEFAULT 'piece',
    fill_amount DECIMAL(10,3) NOT NULL,
    fill_unit VARCHAR(50) NOT NULL,
    package_material ENUM('plastic', 'glass', 'metal', 'paper', 'composite', 'other'),
    is_reusable BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_package_type (package_type),
    INDEX idx_fill_unit (fill_unit),
    INDEX idx_is_active (is_active)
);

-- Lebensmittel table (Extended for Multi-Tenant)
CREATE TABLE IF NOT EXISTS lebensmittel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    menge INT DEFAULT 0,
    einheit VARCHAR(100),
    ablaufdatum DATE,
    kategorie VARCHAR(100),
    ean_code VARCHAR(50),
    mindestmenge INT DEFAULT 0,

    -- Multi-Tenant fields
    household_id INT NOT NULL,
    storage_location_id INT NULL,
    package_id INT NULL,
    package_count INT DEFAULT 1,
    created_by INT NOT NULL,
    updated_by INT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign Keys
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    FOREIGN KEY (storage_location_id) REFERENCES storage_locations(id) ON DELETE SET NULL,
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,

    -- Indexes
    INDEX idx_name (name),
    INDEX idx_kategorie (kategorie),
    INDEX idx_ean_code (ean_code),
    INDEX idx_household_id (household_id),
    INDEX idx_storage_location_id (storage_location_id),
    INDEX idx_package_id (package_id),
    INDEX idx_created_by (created_by),
    INDEX idx_ablaufdatum (ablaufdatum)
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

-- Transactions table (Extended for Multi-Tenant)
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lebensmittel_id INT NOT NULL,
    batch_id INT,
    transaction_type ENUM('purchase', 'consumption') NOT NULL,
    menge INT NOT NULL,
    mhd DATE,
    reason TEXT,
    created_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (lebensmittel_id) REFERENCES lebensmittel(id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES lebensmittel_batches(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,

    INDEX idx_lebensmittel_id (lebensmittel_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_created_at (created_at),
    INDEX idx_created_by (created_by)
);

-- Insert sample data for Multi-Tenant system

-- Create default household
INSERT IGNORE INTO households (id, name, description, is_active) VALUES
(1, 'Demo Haushalt', 'Standard Demo-Haushalt für FoodTrack', TRUE);

-- Insert default admin user (password: admin)
INSERT IGNORE INTO users (id, username, email, password_hash, display_name, household_id, role) VALUES
(1, 'admin', 'admin@foodtrack.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Administrator', 1, 'admin');

-- Update household admin reference
UPDATE households SET admin_user_id = 1 WHERE id = 1;

-- Insert sample packages
INSERT IGNORE INTO packages (id, name, description, package_type, fill_amount, fill_unit, package_material) VALUES
(1, '1 Stück', 'Einzelstück', 'piece', 1.000, 'Stück', NULL),
(2, '500g Packung', 'Standard 500g Packung', 'pack', 500.000, 'g', 'plastic'),
(3, '1L Flasche', '1 Liter Flasche', 'bottle', 1.000, 'L', 'plastic'),
(4, '6er Pack', '6 Stück Packung', 'pack', 6.000, 'Stück', 'cardboard'),
(5, '250g Dose', '250 Gramm Konservendose', 'can', 250.000, 'g', 'metal');

-- Insert sample storage locations
INSERT IGNORE INTO storage_locations (id, name, description, household_id, location_type, temperature_zone) VALUES
(1, 'Kühlschrank', 'Hauptkühlschrank in der Küche', 1, 'refrigerator', 'refrigerated'),
(2, 'Gefrierschrank', 'Gefriertruhe im Keller', 1, 'freezer', 'frozen'),
(3, 'Speisekammer', 'Vorratsschrank in der Küche', 1, 'pantry', 'room_temperature'),
(4, 'Keller', 'Kühler Kellerraum', 1, 'cellar', 'cool');

-- Insert sample lebensmittel
INSERT IGNORE INTO lebensmittel (name, menge, einheit, kategorie, mindestmenge, household_id, storage_location_id, package_id, package_count, created_by) VALUES
('Äpfel', 0, 'Stück', 'Obst', 5, 1, 3, 1, 0, 1),
('Milch', 0, 'Liter', 'Milchprodukte', 2, 1, 1, 3, 0, 1),
('Brot', 0, 'Stück', 'Backwaren', 1, 1, 3, 1, 0, 1);
