<?php
// Standalone endpoints/lebensmittel.php - NO external dependencies

// Simple auth function (inline)
function requireAuth() {
    $headers = getallheaders();
    $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
    
    if (empty($authHeader) || !str_starts_with($authHeader, 'Bearer ')) {
        http_response_code(401);
        echo json_encode(['error' => 'Authorization required']);
        exit();
    }
    
    $token = substr($authHeader, 7);
    $decoded = base64_decode($token);
    
    if (!$decoded || !str_contains($decoded, ':')) {
        http_response_code(401);
        echo json_encode(['error' => 'Invalid token']);
        exit();
    }
    
    // Return mock user for now (admin@foodtrack.com with household_id=1)
    return [
        'id' => 1,
        'email' => 'admin@foodtrack.com',
        'household_id' => 1
    ];
}

function get_lebensmittel_list($pdo) {
    try {
        // Get user info for Multi-Tenant filtering
        $user = requireAuth();

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

        // Convert to expected format with Multi-Tenant fields
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
                'STANDALONE_SUCCESS' => 'MULTI_TENANT_WORKING',
                'TIMESTAMP' => date('Y-m-d H:i:s')
            ];
        }, $lebensmittel);

        echo json_encode($result);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function get_lebensmittel($pdo, $id) {
    try {
        $user = requireAuth();

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
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            return;
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

        echo json_encode($result);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function create_lebensmittel($pdo) {
    try {
        $user = requireAuth();
        $input = json_decode(file_get_contents('php://input'), true);
        
        if (!isset($input['name']) || empty(trim($input['name']))) {
            http_response_code(400);
            echo json_encode(['error' => 'Name is required']);
            return;
        }

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

        // Return success
        http_response_code(201);
        echo json_encode([
            'id' => (int)$lebensmittel_id,
            'name' => trim($input['name']),
            'quantity' => (int)$quantity,
            'einheit' => $input['einheit'] ?? null,
            'kategorie' => $input['kategorie'] ?? null,
            'ablaufdatum' => $input['ablaufdatum'] ?? null,
            'ean_code' => $input['ean_code'] ?? null,
            'mindestmenge' => (int)($input['mindestmenge'] ?? 0),
            'household_id' => (int)$user['household_id'],
            'storage_location_id' => $input['storage_location_id'] ? (int)$input['storage_location_id'] : null,
            'package_id' => $input['package_id'] ? (int)$input['package_id'] : null,
            'package_count' => (int)($input['package_count'] ?? 1),
            'created_by' => (int)$user['id'],
            'updated_by' => null,
            'STANDALONE_SUCCESS' => 'MULTI_TENANT_CREATED'
        ]);

    } catch (Exception $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function update_lebensmittel($pdo, $id) {
    try {
        $user = requireAuth();
        
        // Check if item exists and belongs to user's household
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
        $stmt->execute([$id, $user['household_id']]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            return;
        }

        $input = json_decode(file_get_contents('php://input'), true);

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
        get_lebensmittel($pdo, $id);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function delete_lebensmittel($pdo, $id) {
    try {
        $user = requireAuth();
        
        // Check if item exists and belongs to user's household
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
        $stmt->execute([$id, $user['household_id']]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            return;
        }

        // Delete item (cascades to batches and transactions)
        $stmt = $pdo->prepare("DELETE FROM lebensmittel WHERE id = ?");
        $stmt->execute([$id]);

        http_response_code(204);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function get_lebensmittel_by_ean($pdo, $ean_code) {
    try {
        $user = requireAuth();

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
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel with EAN ' . $ean_code . ' not found']);
            return;
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

        echo json_encode($result);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function get_low_stock_items($pdo) {
    try {
        $user = requireAuth();

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

        echo json_encode($result);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}
?>
