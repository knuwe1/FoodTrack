<?php

require_once 'users.php';

function get_lebensmittel_list($pdo) {
    try {
        // Get user info for Multi-Tenant filtering
        require_once __DIR__ . '/../middleware/auth.php';
        $user = requireAuth();

        $stmt = $pdo->prepare("
            SELECT l.*,
                   COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity
            FROM lebensmittel l
            LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
            WHERE l.household_id = ?
            GROUP BY l.id
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
                'mindestmenge' => (int)$item['mindestmenge']
            ];
        }, $lebensmittel);

        echo json_encode($result);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function get_lebensmittel($pdo, $id) {
    try {
        $stmt = $pdo->prepare("
            SELECT l.*,
                   COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity
            FROM lebensmittel l
            LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
            WHERE l.id = ?
            GROUP BY l.id
        ");
        $stmt->execute([$id]);
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
            'mindestmenge' => (int)$item['mindestmenge']
        ];

        echo json_encode($result);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function create_lebensmittel($pdo) {
    $input = json_decode(file_get_contents('php://input'), true);

    if (!isset($input['name']) || empty(trim($input['name']))) {
        http_response_code(400);
        echo json_encode(['error' => 'Name is required']);
        return;
    }

    try {
        // Get user info for Multi-Tenant
        require_once __DIR__ . '/../middleware/auth.php';
        $user = requireAuth();

        $pdo->beginTransaction();

        // Create lebensmittel with Multi-Tenant fields
        $stmt = $pdo->prepare("
            INSERT INTO lebensmittel (
                name, menge, einheit, ablaufdatum, kategorie, ean_code, mindestmenge,
                household_id, storage_location_id, package_id, package_count, created_by
            )
            VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([
            $input['name'],
            $input['einheit'] ?? null,
            $input['ablaufdatum'] ?? null,
            $input['kategorie'] ?? null,
            $input['ean_code'] ?? null,
            $input['mindestmenge'] ?? 0,
            $user['household_id'],
            $input['storage_location_id'] ?? null,
            $input['package_id'] ?? 1, // Default package
            $input['package_count'] ?? 1,
            $user['id']
        ]);

        $lebensmittel_id = $pdo->lastInsertId();

        // Create initial batch if quantity > 0
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

        // Return created item
        http_response_code(201);
        get_lebensmittel($pdo, $lebensmittel_id);

    } catch (PDOException $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function update_lebensmittel($pdo, $id) {
    $input = json_decode(file_get_contents('php://input'), true);

    try {
        // Check if item exists
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ?");
        $stmt->execute([$id]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            return;
        }

        // Build update query dynamically
        $fields = [];
        $values = [];

        if (isset($input['name'])) {
            $fields[] = 'name = ?';
            $values[] = $input['name'];
        }
        if (isset($input['einheit'])) {
            $fields[] = 'einheit = ?';
            $values[] = $input['einheit'];
        }
        if (isset($input['kategorie'])) {
            $fields[] = 'kategorie = ?';
            $values[] = $input['kategorie'];
        }
        if (isset($input['ablaufdatum'])) {
            $fields[] = 'ablaufdatum = ?';
            $values[] = $input['ablaufdatum'];
        }
        if (isset($input['ean_code'])) {
            $fields[] = 'ean_code = ?';
            $values[] = $input['ean_code'];
        }
        if (isset($input['mindestmenge'])) {
            $fields[] = 'mindestmenge = ?';
            $values[] = $input['mindestmenge'];
        }

        if (empty($fields)) {
            http_response_code(400);
            echo json_encode(['error' => 'No fields to update']);
            return;
        }

        $values[] = $id;
        $sql = "UPDATE lebensmittel SET " . implode(', ', $fields) . " WHERE id = ?";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($values);

        // Return updated item
        get_lebensmittel($pdo, $id);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function delete_lebensmittel($pdo, $id) {
    try {
        // Check if item exists
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ?");
        $stmt->execute([$id]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            return;
        }

        // Delete item (cascades to batches and transactions)
        $stmt = $pdo->prepare("DELETE FROM lebensmittel WHERE id = ?");
        $stmt->execute([$id]);

        http_response_code(204);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function get_lebensmittel_by_ean($pdo, $ean_code) {
    try {
        $stmt = $pdo->prepare("
            SELECT l.*,
                   COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity
            FROM lebensmittel l
            LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
            WHERE l.ean_code = ?
            GROUP BY l.id
        ");
        $stmt->execute([$ean_code]);
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
            'mindestmenge' => (int)$item['mindestmenge']
        ];

        echo json_encode($result);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function get_low_stock_items($pdo) {
    try {
        $stmt = $pdo->prepare("
            SELECT l.*,
                   COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity
            FROM lebensmittel l
            LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
            WHERE l.mindestmenge > 0
            GROUP BY l.id
            HAVING quantity < l.mindestmenge
            ORDER BY l.name
        ");
        $stmt->execute();
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
                'mindestmenge' => (int)$item['mindestmenge']
            ];
        }, $lebensmittel);

        echo json_encode($result);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}