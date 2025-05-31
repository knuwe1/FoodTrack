-- Migration script to convert existing FoodTrack database to Multi-Tenant
-- Run this script to upgrade existing installations

-- Step 1: Create new tables for Multi-Tenant system

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

-- Step 2: Backup existing users table
CREATE TABLE IF NOT EXISTS users_backup AS SELECT * FROM users;

-- Step 3: Modify users table for Multi-Tenant
ALTER TABLE users 
ADD COLUMN username VARCHAR(100) AFTER id,
ADD COLUMN display_name VARCHAR(255) AFTER email,
ADD COLUMN household_id INT AFTER display_name,
ADD COLUMN role ENUM('admin', 'member') DEFAULT 'member' AFTER household_id,
ADD COLUMN last_login_at TIMESTAMP NULL AFTER updated_at;

-- Step 4: Create default household and update existing users
INSERT IGNORE INTO households (id, name, description, is_active) VALUES 
(1, 'Demo Haushalt', 'Standard Demo-Haushalt für bestehende Daten', TRUE);

-- Update existing users to belong to default household
UPDATE users SET 
    username = SUBSTRING(email, 1, LOCATE('@', email) - 1),
    display_name = COALESCE(NULLIF(username, ''), SUBSTRING(email, 1, LOCATE('@', email) - 1)),
    household_id = 1,
    role = 'admin'
WHERE household_id IS NULL;

-- Set first user as household admin
UPDATE households SET admin_user_id = (SELECT MIN(id) FROM users WHERE household_id = 1) WHERE id = 1;

-- Step 5: Add indexes to users table
ALTER TABLE users 
ADD INDEX idx_household_id (household_id),
ADD INDEX idx_email (email),
ADD INDEX idx_role (role);

-- Step 6: Add foreign key constraints
ALTER TABLE users 
ADD CONSTRAINT fk_user_household 
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE;

ALTER TABLE households 
ADD CONSTRAINT fk_household_admin 
    FOREIGN KEY (admin_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE storage_locations 
ADD CONSTRAINT fk_storage_household 
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE;

-- Step 7: Backup existing lebensmittel table
CREATE TABLE IF NOT EXISTS lebensmittel_backup AS SELECT * FROM lebensmittel;

-- Step 8: Modify lebensmittel table for Multi-Tenant
ALTER TABLE lebensmittel 
ADD COLUMN household_id INT NOT NULL DEFAULT 1 AFTER mindestmenge,
ADD COLUMN storage_location_id INT NULL AFTER household_id,
ADD COLUMN package_id INT NULL AFTER storage_location_id,
ADD COLUMN package_count INT DEFAULT 1 AFTER package_id,
ADD COLUMN created_by INT NOT NULL DEFAULT 1 AFTER package_count,
ADD COLUMN updated_by INT NULL AFTER created_by;

-- Step 9: Add indexes to lebensmittel table
ALTER TABLE lebensmittel 
ADD INDEX idx_household_id (household_id),
ADD INDEX idx_storage_location_id (storage_location_id),
ADD INDEX idx_package_id (package_id),
ADD INDEX idx_created_by (created_by),
ADD INDEX idx_ablaufdatum (ablaufdatum);

-- Step 10: Add foreign key constraints to lebensmittel
ALTER TABLE lebensmittel 
ADD CONSTRAINT fk_lebensmittel_household 
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_lebensmittel_storage 
    FOREIGN KEY (storage_location_id) REFERENCES storage_locations(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_lebensmittel_package 
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_lebensmittel_creator 
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
ADD CONSTRAINT fk_lebensmittel_updater 
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;

-- Step 11: Modify transactions table for Multi-Tenant
ALTER TABLE transactions 
ADD COLUMN reason TEXT AFTER mhd,
ADD COLUMN created_by INT NOT NULL DEFAULT 1 AFTER reason;

ALTER TABLE transactions 
ADD INDEX idx_created_by (created_by),
ADD CONSTRAINT fk_transaction_creator 
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT;

-- Step 12: Insert sample data for Multi-Tenant features

-- Insert sample packages
INSERT IGNORE INTO packages (id, name, description, package_type, fill_amount, fill_unit, package_material) VALUES
(1, '1 Stück', 'Einzelstück', 'piece', 1.000, 'Stück', NULL),
(2, '500g Packung', 'Standard 500g Packung', 'pack', 500.000, 'g', 'plastic'),
(3, '1L Flasche', '1 Liter Flasche', 'bottle', 1.000, 'L', 'plastic'),
(4, '6er Pack', '6 Stück Packung', 'pack', 6.000, 'Stück', 'paper'),
(5, '250g Dose', '250 Gramm Konservendose', 'can', 250.000, 'g', 'metal');

-- Insert sample storage locations for default household
INSERT IGNORE INTO storage_locations (id, name, description, household_id, location_type, temperature_zone) VALUES
(1, 'Kühlschrank', 'Hauptkühlschrank in der Küche', 1, 'refrigerator', 'refrigerated'),
(2, 'Gefrierschrank', 'Gefriertruhe im Keller', 1, 'freezer', 'frozen'),
(3, 'Speisekammer', 'Vorratsschrank in der Küche', 1, 'pantry', 'room_temperature'),
(4, 'Keller', 'Kühler Kellerraum', 1, 'cellar', 'cool');

-- Step 13: Update existing lebensmittel with default storage locations
UPDATE lebensmittel SET 
    storage_location_id = CASE 
        WHEN kategorie LIKE '%Milch%' OR kategorie LIKE '%Käse%' OR kategorie LIKE '%Joghurt%' THEN 1  -- Kühlschrank
        WHEN kategorie LIKE '%Tiefkühl%' OR kategorie LIKE '%Eis%' THEN 2  -- Gefrierschrank
        ELSE 3  -- Speisekammer
    END,
    package_id = 1,  -- Default: Einzelstück
    package_count = 1
WHERE storage_location_id IS NULL;

-- Step 14: Verification queries (uncomment to run checks)
-- SELECT 'Households created:' as info, COUNT(*) as count FROM households;
-- SELECT 'Users updated:' as info, COUNT(*) as count FROM users WHERE household_id IS NOT NULL;
-- SELECT 'Storage locations created:' as info, COUNT(*) as count FROM storage_locations;
-- SELECT 'Packages created:' as info, COUNT(*) as count FROM packages;
-- SELECT 'Lebensmittel updated:' as info, COUNT(*) as count FROM lebensmittel WHERE household_id IS NOT NULL;

COMMIT;
