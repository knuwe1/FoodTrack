<?php
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

$user = requireAuth();
$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$pathParts = explode('/', trim($path, '/'));

try {
    switch ($method) {
        case 'GET':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                getLebensmittelById($pathParts[4], $user);
            } else {
                getLebensmittelList($user);
            }
            break;
        case 'POST':
            createLebensmittel($user);
            break;
        default:
            sendError('Method not allowed', 405);
    }
} catch (Exception $e) {
    sendError('Internal server error: ' . $e->getMessage(), 500);
}

function getLebensmittelList($user) {
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
        WHERE l.household_id = ?
        GROUP BY l.id
        ORDER BY l.name
    ");
    $stmt->execute([$user['household_id']]);
    $lebensmittel = $stmt->fetchAll(PDO::FETCH_ASSOC);

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
            'location_type' => $item['location_type'],
            'package_id' => $item['package_id'] ? (int)$item['package_id'] : null,
            'package_name' => $item['package_name'],
            'package_count' => (int)$item['package_count'],
            'package_fill_amount' => $item['fill_amount'] ? (float)$item['fill_amount'] : null,
            'package_fill_unit' => $item['fill_unit'],
            'package_type' => $item['package_type'],
            'created_by' => (int)$item['created_by'],
            'updated_by' => $item['updated_by'] ? (int)$item['updated_by'] : null,
            'REPLACED_SUCCESS' => 'MULTI-TENANT-WORKING',
            'TIMESTAMP' => date('Y-m-d H:i:s')
        ];
    }, $lebensmittel);

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
        'updated_by' => $item['updated_by'] ? (int)$item['updated_by'] : null,
        'REPLACED_SUCCESS' => 'MULTI-TENANT-WORKING',
        'TIMESTAMP' => date('Y-m-d H:i:s')
    ];

    sendSuccess($result);
}

function createLebensmittel($user) {
    global $pdo;
    
    $input = json_decode(file_get_contents('php://input'), true);
    
    if (!isset($input['name']) || empty(trim($input['name']))) {
        sendError('Name is required', 400);
    }

    try {
        $pdo->beginTransaction();

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

        $quantity = $input['quantity'] ?? $input['menge'] ?? 0;
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
        }

        $pdo->commit();
        return getLebensmittelById($lebensmittel_id, $user);

    } catch (PDOException $e) {
        $pdo->rollBack();
        throw $e;
    }
}
?>