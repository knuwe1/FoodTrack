<?php
// php-backend/api/v1/storage-locations.php

require_once __DIR__ . '/../../config.php';
require_once __DIR__ . '/../../middleware/auth.php';
require_once __DIR__ . '/../../utils/response.php';

// Ensure database connection exists
if (!isset($pdo)) {
    $config = require_once __DIR__ . '/../../config.php';
    $dsn = "mysql:host={$config['database']['host']};dbname={$config['database']['name']};charset={$config['database']['charset']}";
    $pdo = new PDO($dsn, $config['database']['user'], $config['database']['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Require authentication for all storage location operations
$user = requireAuth();

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$pathParts = explode('/', trim($path, '/'));

try {
    switch ($method) {
        case 'GET':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // GET /api/v1/storage-locations/{id}
                getStorageLocationById($pathParts[4], $user);
            } else {
                // GET /api/v1/storage-locations/
                getStorageLocations($user);
            }
            break;

        case 'POST':
            // POST /api/v1/storage-locations/
            createStorageLocation($user);
            break;

        case 'PATCH':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // PATCH /api/v1/storage-locations/{id}
                updateStorageLocation($pathParts[4], $user);
            }
            break;

        case 'DELETE':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // DELETE /api/v1/storage-locations/{id}
                deleteStorageLocation($pathParts[4], $user);
            }
            break;

        default:
            sendError('Method not allowed', 405);
    }
} catch (Exception $e) {
    error_log("Storage Location API Error: " . $e->getMessage());
    sendError('Internal server error: ' . $e->getMessage(), 500);
}

function getStorageLocations($user) {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT sl.*,
               (SELECT COUNT(*) FROM lebensmittel WHERE storage_location_id = sl.id) as item_count
        FROM storage_locations sl
        WHERE sl.household_id = ? AND sl.is_active = 1
        ORDER BY sl.name ASC
    ");
    $stmt->execute([$user['household_id']]);
    $locations = $stmt->fetchAll(PDO::FETCH_ASSOC);

    sendSuccess($locations);
}

function getStorageLocationById($locationId, $user) {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT sl.*,
               (SELECT COUNT(*) FROM lebensmittel WHERE storage_location_id = sl.id) as item_count
        FROM storage_locations sl
        WHERE sl.id = ? AND sl.household_id = ? AND sl.is_active = 1
    ");
    $stmt->execute([$locationId, $user['household_id']]);
    $location = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$location) {
        sendError('Storage location not found', 404);
    }

    sendSuccess($location);
}

function createStorageLocation($user) {
    global $pdo;

    // Only admins can create storage locations
    if ($user['role'] !== 'admin') {
        sendError('Access denied. Only household admins can create storage locations.', 403);
    }

    $input = json_decode(file_get_contents('php://input'), true);

    if (!isset($input['name']) || empty(trim($input['name']))) {
        sendError('Storage location name is required', 400);
    }

    $validLocationTypes = ['pantry', 'refrigerator', 'freezer', 'cellar', 'garage', 'other'];
    $validTemperatureZones = ['frozen', 'refrigerated', 'cool', 'room_temperature', 'warm'];

    $locationType = $input['location_type'] ?? 'pantry';
    $temperatureZone = $input['temperature_zone'] ?? 'room_temperature';

    if (!in_array($locationType, $validLocationTypes)) {
        sendError('Invalid location type', 400);
    }

    if (!in_array($temperatureZone, $validTemperatureZones)) {
        sendError('Invalid temperature zone', 400);
    }

    $stmt = $pdo->prepare("
        INSERT INTO storage_locations (name, description, household_id, location_type, temperature_zone, is_active)
        VALUES (?, ?, ?, ?, ?, 1)
    ");
    $stmt->execute([
        trim($input['name']),
        $input['description'] ?? null,
        $user['household_id'],
        $locationType,
        $temperatureZone
    ]);

    $locationId = $pdo->lastInsertId();
    getStorageLocationById($locationId, $user);
}

function updateStorageLocation($locationId, $user) {
    global $pdo;

    // Only admins can update storage locations
    if ($user['role'] !== 'admin') {
        sendError('Access denied. Only household admins can update storage locations.', 403);
    }

    // Check if location belongs to user's household
    $stmt = $pdo->prepare("SELECT id FROM storage_locations WHERE id = ? AND household_id = ?");
    $stmt->execute([$locationId, $user['household_id']]);
    if (!$stmt->fetch()) {
        sendError('Storage location not found', 404);
    }

    $input = json_decode(file_get_contents('php://input'), true);

    $updates = [];
    $params = [];

    if (isset($input['name']) && !empty(trim($input['name']))) {
        $updates[] = "name = ?";
        $params[] = trim($input['name']);
    }

    if (isset($input['description'])) {
        $updates[] = "description = ?";
        $params[] = $input['description'];
    }

    if (isset($input['location_type'])) {
        $validLocationTypes = ['pantry', 'refrigerator', 'freezer', 'cellar', 'garage', 'other'];
        if (!in_array($input['location_type'], $validLocationTypes)) {
            sendError('Invalid location type', 400);
        }
        $updates[] = "location_type = ?";
        $params[] = $input['location_type'];
    }

    if (isset($input['temperature_zone'])) {
        $validTemperatureZones = ['frozen', 'refrigerated', 'cool', 'room_temperature', 'warm'];
        if (!in_array($input['temperature_zone'], $validTemperatureZones)) {
            sendError('Invalid temperature zone', 400);
        }
        $updates[] = "temperature_zone = ?";
        $params[] = $input['temperature_zone'];
    }

    if (isset($input['is_active'])) {
        $updates[] = "is_active = ?";
        $params[] = $input['is_active'] ? 1 : 0;
    }

    if (empty($updates)) {
        sendError('No valid fields to update', 400);
    }

    $params[] = $locationId;

    $stmt = $pdo->prepare("
        UPDATE storage_locations
        SET " . implode(', ', $updates) . ", updated_at = NOW()
        WHERE id = ?
    ");
    $stmt->execute($params);

    getStorageLocationById($locationId, $user);
}

function deleteStorageLocation($locationId, $user) {
    global $pdo;

    // Only admins can delete storage locations
    if ($user['role'] !== 'admin') {
        sendError('Access denied. Only household admins can delete storage locations.', 403);
    }

    // Check if location belongs to user's household
    $stmt = $pdo->prepare("SELECT id FROM storage_locations WHERE id = ? AND household_id = ?");
    $stmt->execute([$locationId, $user['household_id']]);
    if (!$stmt->fetch()) {
        sendError('Storage location not found', 404);
    }

    // Check if location is in use
    $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM lebensmittel WHERE storage_location_id = ?");
    $stmt->execute([$locationId]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($result['count'] > 0) {
        sendError('Cannot delete storage location. It contains ' . $result['count'] . ' items.', 400);
    }

    $stmt = $pdo->prepare("
        UPDATE storage_locations
        SET is_active = 0
        WHERE id = ?
    ");
    $stmt->execute([$locationId]);

    sendSuccess(['message' => 'Storage location deleted successfully']);
}
?>
