<?php
// php-backend/api/v1/lebensmittel.php - Multi-Tenant Version

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../middleware/auth.php';
require_once __DIR__ . '/../../utils/response.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Require authentication for all lebensmittel operations
$user = requireAuth();

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$pathParts = explode('/', trim($path, '/'));

try {
    switch ($method) {
        case 'GET':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // GET /api/v1/lebensmittel/{id}
                getLebensmittelById($pathParts[4], $user);
            } elseif (isset($pathParts[4]) && $pathParts[4] === 'low-stock') {
                // GET /api/v1/lebensmittel/low-stock
                getLowStockItems($user);
            } elseif (isset($pathParts[4]) && $pathParts[4] === 'ean' && isset($pathParts[5])) {
                // GET /api/v1/lebensmittel/ean/{ean_code}
                getLebensmittelByEan($pathParts[5], $user);
            } else {
                // GET /api/v1/lebensmittel/
                getLebensmittelList($user);
            }
            break;

        case 'POST':
            // POST /api/v1/lebensmittel/
            createLebensmittel($user);
            break;

        case 'PATCH':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // PATCH /api/v1/lebensmittel/{id}
                updateLebensmittel($pathParts[4], $user);
            }
            break;

        case 'DELETE':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // DELETE /api/v1/lebensmittel/{id}
                deleteLebensmittel($pathParts[4], $user);
            }
            break;

        default:
            sendError('Method not allowed', 405);
    }
} catch (Exception $e) {
    error_log("Lebensmittel API Error: " . $e->getMessage());
    sendError('Internal server error: ' . $e->getMessage(), 500);
}

function getLebensmittelList($user) {
    global $pdo;

    // DEBUG: Log function call
    error_log("DEBUG: getLebensmittelList called for household_id: " . $user['household_id']);

    $stmt = $pdo->prepare("
        SELECT l.*,
               COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity,
               sl.name as storage_location_name,
               sl.location_type,
               p.name as package_name,
               p.fill_amount,
               p.fill_unit,
               p.package_type
        FROM lebensmittel l
        LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        WHERE l.household_id = ?
        GROUP BY l.id
        ORDER BY l.name
    ");
    $stmt->execute([$user['household_id']]);
    $lebensmittel = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // DEBUG: Log raw SQL result
    error_log("DEBUG: Raw SQL result count: " . count($lebensmittel));
    if (count($lebensmittel) > 0) {
        error_log("DEBUG: First item raw: " . json_encode($lebensmittel[0]));
    }

    // Convert to expected format with multi-tenant fields
    $result = array_map(function($item) {
        $formatted = [
            'id' => (int)$item['id'],
            'name' => $item['name'],
            'quantity' => (int)$item['quantity'],
            'einheit' => $item['einheit'],
            'kategorie' => $item['kategorie'],
            'ablaufdatum' => $item['ablaufdatum'],
            'ean_code' => $item['ean_code'],
            'mindestmenge' => (int)$item['mindestmenge'],
            'household_id' => (int)$item['household_id'],
            'storage_location_id' => $item['storage_location_id'] ? (int)$item['storage_location_id'] : null,
            'storage_location_name' => $item['storage_location_name'],
            'location_type' => $item['location_type'],
            'package_id' => $item['package_id'] ? (int)$item['package_id'] : null,
            'package_name' => $item['package_name'],
            'package_count' => (int)$item['package_count'],
            'package_fill_amount' => $item['fill_amount'] ? (float)$item['fill_amount'] : null,
            'package_fill_unit' => $item['fill_unit'],
            'package_type' => $item['package_type'],
            'created_by' => (int)$item['created_by'],
            'updated_by' => $item['updated_by'] ? (int)$item['updated_by'] : null
        ];

        // DEBUG: Log formatted item
        error_log("DEBUG: Formatted item " . $item['id'] . ": " . json_encode($formatted));
        return $formatted;
    }, $lebensmittel);

    // DEBUG: Log final result
    error_log("DEBUG: Final result count: " . count($result));

    // TEMPORARY: Add debug info to response
    if (count($result) > 0) {
        $result[0]['DEBUG_VERSION'] = 'Multi-Tenant-v2.0';
        $result[0]['DEBUG_TIME'] = date('Y-m-d H:i:s');
    }

    sendSuccess($result);
}

function getLebensmittelById($id, $user) {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT l.*,
               COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity,
               sl.name as storage_location_name,
               sl.location_type,
               p.name as package_name,
               p.fill_amount,
               p.fill_unit,
               p.package_type
        FROM lebensmittel l
        LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        WHERE l.id = ? AND l.household_id = ?
        GROUP BY l.id
    ");
    $stmt->execute([$id, $user['household_id']]);
    $item = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$item) {
        sendError('Lebensmittel not found', 404);
    }

    $result = [
        'id' => (int)$item['id'],
        'name' => $item['name'],
        'quantity' => (int)$item['quantity'],
        'einheit' => $item['einheit'],
        'kategorie' => $item['kategorie'],
        'ablaufdatum' => $item['ablaufdatum'],
        'ean_code' => $item['ean_code'],
        'mindestmenge' => (int)$item['mindestmenge'],
        'household_id' => (int)$item['household_id'],
        'storage_location_id' => $item['storage_location_id'] ? (int)$item['storage_location_id'] : null,
        'storage_location_name' => $item['storage_location_name'],
        'location_type' => $item['location_type'],
        'package_id' => $item['package_id'] ? (int)$item['package_id'] : null,
        'package_name' => $item['package_name'],
        'package_count' => (int)$item['package_count'],
        'package_fill_amount' => $item['fill_amount'] ? (float)$item['fill_amount'] : null,
        'package_fill_unit' => $item['fill_unit'],
        'package_type' => $item['package_type'],
        'created_by' => (int)$item['created_by'],
        'updated_by' => $item['updated_by'] ? (int)$item['updated_by'] : null
    ];

    sendSuccess($result);
}

function createLebensmittel($user) {
    global $pdo;

    $input = json_decode(file_get_contents('php://input'), true);

    // DEBUG: Log input data
    error_log("DEBUG: createLebensmittel input: " . json_encode($input));
    error_log("DEBUG: createLebensmittel user: " . json_encode($user));

    if (!isset($input['name']) || empty(trim($input['name']))) {
        sendError('Name is required', 400);
    }

    // Validate storage location belongs to user's household
    if (isset($input['storage_location_id']) && $input['storage_location_id']) {
        $stmt = $pdo->prepare("SELECT id FROM storage_locations WHERE id = ? AND household_id = ? AND is_active = 1");
        $stmt->execute([$input['storage_location_id'], $user['household_id']]);
        if (!$stmt->fetch()) {
            sendError('Invalid storage location', 400);
        }
    }

    // Validate package exists
    if (isset($input['package_id']) && $input['package_id']) {
        $stmt = $pdo->prepare("SELECT id FROM packages WHERE id = ? AND is_active = 1");
        $stmt->execute([$input['package_id']]);
        if (!$stmt->fetch()) {
            sendError('Invalid package', 400);
        }
    }

    try {
        $pdo->beginTransaction();

        // Create lebensmittel with multi-tenant fields
        $stmt = $pdo->prepare("
            INSERT INTO lebensmittel (name, menge, einheit, ablaufdatum, kategorie, ean_code, mindestmenge,
                                    household_id, storage_location_id, package_id, package_count, created_by)
            VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([
            trim($input['name']),
            $input['einheit'] ?? null,
            $input['ablaufdatum'] ?? null,
            $input['kategorie'] ?? null,
            $input['ean_code'] ?? null,
            $input['mindestmenge'] ?? 0,
            $user['household_id'],
            $input['storage_location_id'] ?? null,
            $input['package_id'] ?? null,
            $input['package_count'] ?? 1,
            $user['id']
        ]);

        $lebensmittel_id = $pdo->lastInsertId();

        // DEBUG: Log created ID
        error_log("DEBUG: Created lebensmittel_id: " . $lebensmittel_id);

        // Create initial batch if quantity > 0
        $quantity = $input['quantity'] ?? $input['menge'] ?? 0;
        error_log("DEBUG: Creating batch with quantity: " . $quantity);

        if ($quantity > 0) {
            $stmt = $pdo->prepare("
                INSERT INTO lebensmittel_batches (lebensmittel_id, menge, ablaufdatum)
                VALUES (?, ?, ?)
            ");
            $stmt->execute([
                $lebensmittel_id,
                $quantity,
                $input['ablaufdatum'] ?? null
            ]);
            error_log("DEBUG: Batch created successfully");
        }

        $pdo->commit();
        error_log("DEBUG: Transaction committed, calling getLebensmittelById");

        // Return created item
        return getLebensmittelById($lebensmittel_id, $user);

    } catch (PDOException $e) {
        $pdo->rollBack();
        throw $e;
    }
}

function updateLebensmittel($id, $user) {
    global $pdo;

    // Check if item exists and belongs to user's household
    $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
    $stmt->execute([$id, $user['household_id']]);
    if (!$stmt->fetch()) {
        sendError('Lebensmittel not found', 404);
    }

    $input = json_decode(file_get_contents('php://input'), true);

    // Validate storage location if provided
    if (isset($input['storage_location_id']) && $input['storage_location_id']) {
        $stmt = $pdo->prepare("SELECT id FROM storage_locations WHERE id = ? AND household_id = ? AND is_active = 1");
        $stmt->execute([$input['storage_location_id'], $user['household_id']]);
        if (!$stmt->fetch()) {
            sendError('Invalid storage location', 400);
        }
    }

    // Validate package if provided
    if (isset($input['package_id']) && $input['package_id']) {
        $stmt = $pdo->prepare("SELECT id FROM packages WHERE id = ? AND is_active = 1");
        $stmt->execute([$input['package_id']]);
        if (!$stmt->fetch()) {
            sendError('Invalid package', 400);
        }
    }

    // Build update query dynamically
    $fields = [];
    $values = [];

    $allowedFields = ['name', 'einheit', 'kategorie', 'ablaufdatum', 'ean_code', 'mindestmenge',
                     'storage_location_id', 'package_id', 'package_count'];

    foreach ($allowedFields as $field) {
        if (isset($input[$field])) {
            $fields[] = $field . ' = ?';
            $values[] = $input[$field];
        }
    }

    if (!empty($fields)) {
        $fields[] = 'updated_by = ?';
        $values[] = $user['id'];

        $values[] = $id;
        $sql = "UPDATE lebensmittel SET " . implode(', ', $fields) . " WHERE id = ?";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($values);
    }

    // Return updated item
    return getLebensmittelById($id, $user);
}

function deleteLebensmittel($id, $user) {
    global $pdo;

    // Check if item exists and belongs to user's household
    $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
    $stmt->execute([$id, $user['household_id']]);
    if (!$stmt->fetch()) {
        sendError('Lebensmittel not found', 404);
    }

    // Delete item (cascades to batches and transactions)
    $stmt = $pdo->prepare("DELETE FROM lebensmittel WHERE id = ?");
    $stmt->execute([$id]);

    sendSuccess(['message' => 'Lebensmittel deleted successfully']);
}

function getLebensmittelByEan($ean_code, $user) {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT l.*,
               COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity,
               sl.name as storage_location_name,
               p.name as package_name
        FROM lebensmittel l
        LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        WHERE l.ean_code = ? AND l.household_id = ?
        GROUP BY l.id
    ");
    $stmt->execute([$ean_code, $user['household_id']]);
    $item = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$item) {
        sendError('Lebensmittel with EAN ' . $ean_code . ' not found', 404);
    }

    $result = [
        'id' => (int)$item['id'],
        'name' => $item['name'],
        'quantity' => (int)$item['quantity'],
        'einheit' => $item['einheit'],
        'kategorie' => $item['kategorie'],
        'ablaufdatum' => $item['ablaufdatum'],
        'ean_code' => $item['ean_code'],
        'mindestmenge' => (int)$item['mindestmenge'],
        'household_id' => (int)$item['household_id'],
        'storage_location_id' => $item['storage_location_id'] ? (int)$item['storage_location_id'] : null,
        'storage_location_name' => $item['storage_location_name'],
        'package_id' => $item['package_id'] ? (int)$item['package_id'] : null,
        'package_name' => $item['package_name'],
        'package_count' => (int)$item['package_count']
    ];

    sendSuccess($result);
}

function getLowStockItems($user) {
    global $pdo;

    $stmt = $pdo->prepare("
        SELECT l.*,
               COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity,
               sl.name as storage_location_name,
               p.name as package_name
        FROM lebensmittel l
        LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        WHERE l.household_id = ? AND l.mindestmenge > 0
        GROUP BY l.id
        HAVING quantity < l.mindestmenge
        ORDER BY l.name
    ");
    $stmt->execute([$user['household_id']]);
    $lebensmittel = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Convert to expected format
    $result = array_map(function($item) {
        return [
            'id' => (int)$item['id'],
            'name' => $item['name'],
            'quantity' => (int)$item['quantity'],
            'einheit' => $item['einheit'],
            'kategorie' => $item['kategorie'],
            'ablaufdatum' => $item['ablaufdatum'],
            'ean_code' => $item['ean_code'],
            'mindestmenge' => (int)$item['mindestmenge'],
            'household_id' => (int)$item['household_id'],
            'storage_location_id' => $item['storage_location_id'] ? (int)$item['storage_location_id'] : null,
            'storage_location_name' => $item['storage_location_name'],
            'package_id' => $item['package_id'] ? (int)$item['package_id'] : null,
            'package_name' => $item['package_name'],
            'package_count' => (int)$item['package_count']
        ];
    }, $lebensmittel);

    sendSuccess($result);
}
?>
