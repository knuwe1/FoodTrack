<?php
// php-backend/api/v1/packages.php

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

// Require authentication for all package operations
$user = requireAuth();

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$pathParts = explode('/', trim($path, '/'));

try {
    switch ($method) {
        case 'GET':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // GET /api/v1/packages/{id}
                getPackageById($pathParts[4]);
            } else {
                // GET /api/v1/packages/
                getPackages();
            }
            break;

        case 'POST':
            // POST /api/v1/packages/
            createPackage($user);
            break;

        case 'PATCH':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // PATCH /api/v1/packages/{id}
                updatePackage($pathParts[4], $user);
            }
            break;

        case 'DELETE':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // DELETE /api/v1/packages/{id}
                deletePackage($pathParts[4], $user);
            }
            break;

        default:
            sendError('Method not allowed', 405);
    }
} catch (Exception $e) {
    error_log("Package API Error: " . $e->getMessage());
    sendError('Internal server error: ' . $e->getMessage(), 500);
}

function getPackages() {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT p.*,
               (SELECT COUNT(*) FROM lebensmittel WHERE package_id = p.id) as usage_count
        FROM packages p
        WHERE p.is_active = 1
        ORDER BY p.package_type ASC, p.name ASC
    ");
    $stmt->execute();
    $packages = $stmt->fetchAll(PDO::FETCH_ASSOC);

    sendSuccess($packages);
}

function getPackageById($packageId) {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT p.*,
               (SELECT COUNT(*) FROM lebensmittel WHERE package_id = p.id) as usage_count
        FROM packages p
        WHERE p.id = ? AND p.is_active = 1
    ");
    $stmt->execute([$packageId]);
    $package = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$package) {
        sendError('Package not found', 404);
    }

    sendSuccess($package);
}

function createPackage($user) {
    global $pdo;

    // Only admins can create packages (global resource)
    if ($user['role'] !== 'admin') {
        sendError('Access denied. Only admins can create packages.', 403);
    }

    $input = json_decode(file_get_contents('php://input'), true);

    if (!isset($input['name']) || empty(trim($input['name']))) {
        sendError('Package name is required', 400);
    }

    if (!isset($input['fill_amount']) || !is_numeric($input['fill_amount']) || $input['fill_amount'] <= 0) {
        sendError('Valid fill amount is required', 400);
    }

    if (!isset($input['fill_unit']) || empty(trim($input['fill_unit']))) {
        sendError('Fill unit is required', 400);
    }

    $validPackageTypes = ['piece', 'pack', 'bottle', 'can', 'jar', 'bag', 'box', 'tube', 'container', 'bulk'];
    $validMaterials = ['plastic', 'glass', 'metal', 'paper', 'composite', 'other'];

    $packageType = $input['package_type'] ?? 'piece';
    $packageMaterial = $input['package_material'] ?? null;

    if (!in_array($packageType, $validPackageTypes)) {
        sendError('Invalid package type', 400);
    }

    if ($packageMaterial && !in_array($packageMaterial, $validMaterials)) {
        sendError('Invalid package material', 400);
    }

    $stmt = $pdo->prepare("
        INSERT INTO packages (name, description, package_type, fill_amount, fill_unit, package_material, is_reusable, is_active)
        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
    ");
    $stmt->execute([
        trim($input['name']),
        $input['description'] ?? null,
        $packageType,
        $input['fill_amount'],
        trim($input['fill_unit']),
        $packageMaterial,
        isset($input['is_reusable']) ? ($input['is_reusable'] ? 1 : 0) : 0
    ]);

    $packageId = $pdo->lastInsertId();
    getPackageById($packageId);
}

function updatePackage($packageId, $user) {
    global $pdo;

    // Only admins can update packages (global resource)
    if ($user['role'] !== 'admin') {
        sendError('Access denied. Only admins can update packages.', 403);
    }

    // Check if package exists
    $stmt = $pdo->prepare("SELECT id FROM packages WHERE id = ? AND is_active = 1");
    $stmt->execute([$packageId]);
    if (!$stmt->fetch()) {
        sendError('Package not found', 404);
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

    if (isset($input['package_type'])) {
        $validPackageTypes = ['piece', 'pack', 'bottle', 'can', 'jar', 'bag', 'box', 'tube', 'container', 'bulk'];
        if (!in_array($input['package_type'], $validPackageTypes)) {
            sendError('Invalid package type', 400);
        }
        $updates[] = "package_type = ?";
        $params[] = $input['package_type'];
    }

    if (isset($input['fill_amount'])) {
        if (!is_numeric($input['fill_amount']) || $input['fill_amount'] <= 0) {
            sendError('Valid fill amount is required', 400);
        }
        $updates[] = "fill_amount = ?";
        $params[] = $input['fill_amount'];
    }

    if (isset($input['fill_unit']) && !empty(trim($input['fill_unit']))) {
        $updates[] = "fill_unit = ?";
        $params[] = trim($input['fill_unit']);
    }

    if (isset($input['package_material'])) {
        $validMaterials = ['plastic', 'glass', 'metal', 'paper', 'composite', 'other'];
        if ($input['package_material'] && !in_array($input['package_material'], $validMaterials)) {
            sendError('Invalid package material', 400);
        }
        $updates[] = "package_material = ?";
        $params[] = $input['package_material'];
    }

    if (isset($input['is_reusable'])) {
        $updates[] = "is_reusable = ?";
        $params[] = $input['is_reusable'] ? 1 : 0;
    }

    if (isset($input['is_active'])) {
        $updates[] = "is_active = ?";
        $params[] = $input['is_active'] ? 1 : 0;
    }

    if (empty($updates)) {
        sendError('No valid fields to update', 400);
    }

    $params[] = $packageId;

    $stmt = $pdo->prepare("
        UPDATE packages
        SET " . implode(', ', $updates) . ", updated_at = NOW()
        WHERE id = ?
    ");
    $stmt->execute($params);

    getPackageById($packageId);
}

function deletePackage($packageId, $user) {
    global $pdo;

    // Only admins can delete packages (global resource)
    if ($user['role'] !== 'admin') {
        sendError('Access denied. Only admins can delete packages.', 403);
    }

    // Check if package exists
    $stmt = $pdo->prepare("SELECT id FROM packages WHERE id = ? AND is_active = 1");
    $stmt->execute([$packageId]);
    if (!$stmt->fetch()) {
        sendError('Package not found', 404);
    }

    // Check if package is in use
    $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM lebensmittel WHERE package_id = ?");
    $stmt->execute([$packageId]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($result['count'] > 0) {
        sendError('Cannot delete package. It is used by ' . $result['count'] . ' items.', 400);
    }

    $stmt = $pdo->prepare("
        UPDATE packages
        SET is_active = 0
        WHERE id = ?
    ");
    $stmt->execute([$packageId]);

    sendSuccess(['message' => 'Package deleted successfully']);
}
?>
