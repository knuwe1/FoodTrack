<?php
// Standalone endpoints/transactions.php with Multi-Tenant support

// Simple auth function (inline) - same as in lebensmittel.php
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

function get_transactions($pdo) {
    try {
        $user = requireAuth();
        $lebensmittel_id = $_GET['lebensmittel_id'] ?? null;

        $sql = "
            SELECT t.*, l.name as lebensmittel_name
            FROM transactions t
            JOIN lebensmittel l ON t.lebensmittel_id = l.id
            WHERE l.household_id = ?
        ";
        $params = [$user['household_id']];

        if ($lebensmittel_id) {
            $sql .= " AND t.lebensmittel_id = ?";
            $params[] = $lebensmittel_id;
        }

        $sql .= " ORDER BY t.created_at DESC LIMIT 100";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $transactions = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Convert to expected format
        $result = array_map(function($item) {
            return [
                'id' => (int)$item['id'],
                'lebensmittel_id' => (int)$item['lebensmittel_id'],
                'lebensmittel_name' => $item['lebensmittel_name'],
                'batch_id' => $item['batch_id'] ? (int)$item['batch_id'] : null,
                'transaction_type' => $item['transaction_type'],
                'menge' => (int)$item['menge'],
                'mhd' => $item['mhd'],
                'created_at' => $item['created_at']
            ];
        }, $transactions);

        echo json_encode($result);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function create_transaction($pdo) {
    $user = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);

    // Support both Android format and direct format
    $lebensmittel_id = $input['lebensmittel_id'] ?? null;
    $transaction_type = $input['transaction_type'] ?? null;
    $menge = $input['quantity_change'] ?? $input['menge'] ?? null;
    $reason = $input['reason'] ?? null;
    $mhd = $input['mhd'] ?? null;

    if (!$lebensmittel_id || !$transaction_type || !$menge) {
        http_response_code(400);
        echo json_encode(['error' => 'lebensmittel_id, transaction_type, and quantity_change are required']);
        return;
    }

    $lebensmittel_id = (int)$lebensmittel_id;
    $menge = (int)$menge;

    // Convert Android transaction types to lowercase
    $transaction_type = strtolower($transaction_type);

    if (!in_array($transaction_type, ['purchase', 'consumption'])) {
        http_response_code(400);
        echo json_encode(['error' => 'transaction_type must be purchase or consumption']);
        return;
    }

    if ($menge <= 0) {
        http_response_code(400);
        echo json_encode(['error' => 'menge must be positive']);
        return;
    }

    try {
        $pdo->beginTransaction();

        // Check if lebensmittel exists AND belongs to user's household
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
        $stmt->execute([$lebensmittel_id, $user['household_id']]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            $pdo->rollBack();
            return;
        }

        if ($transaction_type === 'purchase') {
            // Create new batch
            $stmt = $pdo->prepare("
                INSERT INTO lebensmittel_batches (lebensmittel_id, menge, ablaufdatum)
                VALUES (?, ?, ?)
            ");
            $stmt->execute([$lebensmittel_id, $menge, $mhd]);
            $batch_id = $pdo->lastInsertId();

            // Create transaction record
            $stmt = $pdo->prepare("
                INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
                VALUES (?, ?, ?, ?, ?)
            ");
            $stmt->execute([$lebensmittel_id, $batch_id, $transaction_type, $menge, $mhd]);

        } else { // consumption
            // FIFO consumption from oldest batches
            $remaining_to_consume = $menge;

            // Get batches ordered by expiration date (FIFO)
            $stmt = $pdo->prepare("
                SELECT id, menge, ablaufdatum
                FROM lebensmittel_batches
                WHERE lebensmittel_id = ? AND menge > 0
                ORDER BY ablaufdatum ASC, id ASC
            ");
            $stmt->execute([$lebensmittel_id]);
            $batches = $stmt->fetchAll(PDO::FETCH_ASSOC);

            if (empty($batches)) {
                http_response_code(400);
                echo json_encode(['error' => 'No stock available for consumption']);
                $pdo->rollBack();
                return;
            }

            $total_available = array_sum(array_column($batches, 'menge'));
            if ($total_available < $remaining_to_consume) {
                http_response_code(400);
                echo json_encode(['error' => 'Insufficient stock. Available: ' . $total_available]);
                $pdo->rollBack();
                return;
            }

            foreach ($batches as $batch) {
                if ($remaining_to_consume <= 0) break;

                $batch_id = $batch['id'];
                $batch_menge = $batch['menge'];
                $consume_from_batch = min($remaining_to_consume, $batch_menge);

                // Update batch quantity
                $new_batch_menge = $batch_menge - $consume_from_batch;
                $stmt = $pdo->prepare("UPDATE lebensmittel_batches SET menge = ? WHERE id = ?");
                $stmt->execute([$new_batch_menge, $batch_id]);

                // Create transaction record for this batch
                $stmt = $pdo->prepare("
                    INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
                    VALUES (?, ?, ?, ?, ?)
                ");
                $stmt->execute([$lebensmittel_id, $batch_id, $transaction_type, $consume_from_batch, $batch['ablaufdatum']]);

                $remaining_to_consume -= $consume_from_batch;
            }
        }

        $pdo->commit();

        http_response_code(201);
        echo json_encode([
            'message' => 'Transaction created successfully',
            'STANDALONE_TRANSACTIONS_SUCCESS' => 'MULTI_TENANT_WORKING'
        ]);

    } catch (PDOException $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function record_purchase($pdo, $lebensmittel_id) {
    $user = requireAuth();
    $quantity = $_GET['quantity'] ?? null;
    $reason = $_GET['reason'] ?? null;
    $mhd = $_GET['mhd'] ?? null;

    if (!$quantity || $quantity <= 0) {
        http_response_code(400);
        echo json_encode(['error' => 'quantity parameter is required and must be positive']);
        return;
    }

    try {
        $pdo->beginTransaction();

        // Check if lebensmittel exists AND belongs to user's household
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
        $stmt->execute([$lebensmittel_id, $user['household_id']]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            $pdo->rollBack();
            return;
        }

        // Create new batch
        $stmt = $pdo->prepare("
            INSERT INTO lebensmittel_batches (lebensmittel_id, menge, ablaufdatum)
            VALUES (?, ?, ?)
        ");
        $stmt->execute([$lebensmittel_id, $quantity, $mhd]);
        $batch_id = $pdo->lastInsertId();

        // Create transaction record
        $stmt = $pdo->prepare("
            INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
            VALUES (?, ?, 'purchase', ?, ?)
        ");
        $stmt->execute([$lebensmittel_id, $batch_id, $quantity, $mhd]);
        $transaction_id = $pdo->lastInsertId();

        $pdo->commit();

        // Return transaction in expected format
        $result = [
            'id' => (int)$transaction_id,
            'lebensmittel_id' => (int)$lebensmittel_id,
            'transaction_type' => 'PURCHASE',
            'quantity_change' => (int)$quantity,
            'quantity_before' => null,
            'quantity_after' => null,
            'reason' => $reason,
            'created_at' => date('Y-m-d H:i:s'),
            'STANDALONE_TRANSACTIONS_SUCCESS' => 'PURCHASE_WORKING'
        ];

        http_response_code(201);
        echo json_encode($result);

    } catch (PDOException $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function record_consumption($pdo, $lebensmittel_id) {
    $user = requireAuth();
    $quantity = $_GET['quantity'] ?? null;
    $reason = $_GET['reason'] ?? null;

    if (!$quantity || $quantity <= 0) {
        http_response_code(400);
        echo json_encode(['error' => 'quantity parameter is required and must be positive']);
        return;
    }

    try {
        $pdo->beginTransaction();

        // Check if lebensmittel exists AND belongs to user's household
        $stmt = $pdo->prepare("SELECT id FROM lebensmittel WHERE id = ? AND household_id = ?");
        $stmt->execute([$lebensmittel_id, $user['household_id']]);
        if (!$stmt->fetch()) {
            http_response_code(404);
            echo json_encode(['error' => 'Lebensmittel not found']);
            $pdo->rollBack();
            return;
        }

        // FIFO consumption from oldest batches
        $remaining_to_consume = (int)$quantity;

        // Get batches ordered by expiration date (FIFO)
        $stmt = $pdo->prepare("
            SELECT id, menge, ablaufdatum
            FROM lebensmittel_batches
            WHERE lebensmittel_id = ? AND menge > 0
            ORDER BY ablaufdatum ASC, id ASC
        ");
        $stmt->execute([$lebensmittel_id]);
        $batches = $stmt->fetchAll(PDO::FETCH_ASSOC);

        if (empty($batches)) {
            http_response_code(400);
            echo json_encode(['error' => 'No stock available for consumption']);
            $pdo->rollBack();
            return;
        }

        $total_available = array_sum(array_column($batches, 'menge'));
        if ($total_available < $remaining_to_consume) {
            http_response_code(400);
            echo json_encode(['error' => 'Insufficient stock. Available: ' . $total_available]);
            $pdo->rollBack();
            return;
        }

        $transaction_id = null;
        foreach ($batches as $batch) {
            if ($remaining_to_consume <= 0) break;

            $batch_id = $batch['id'];
            $batch_menge = $batch['menge'];
            $consume_from_batch = min($remaining_to_consume, $batch_menge);

            // Update batch quantity
            $new_batch_menge = $batch_menge - $consume_from_batch;
            $stmt = $pdo->prepare("UPDATE lebensmittel_batches SET menge = ? WHERE id = ?");
            $stmt->execute([$new_batch_menge, $batch_id]);

            // Create transaction record for this batch
            $stmt = $pdo->prepare("
                INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
                VALUES (?, ?, 'consumption', ?, ?)
            ");
            $stmt->execute([$lebensmittel_id, $batch_id, $consume_from_batch, $batch['ablaufdatum']]);

            if (!$transaction_id) {
                $transaction_id = $pdo->lastInsertId();
            }

            $remaining_to_consume -= $consume_from_batch;
        }

        $pdo->commit();

        // Return transaction in expected format
        $result = [
            'id' => (int)$transaction_id,
            'lebensmittel_id' => (int)$lebensmittel_id,
            'transaction_type' => 'CONSUMPTION',
            'quantity_change' => -(int)$quantity,
            'quantity_before' => null,
            'quantity_after' => null,
            'reason' => $reason,
            'created_at' => date('Y-m-d H:i:s'),
            'STANDALONE_TRANSACTIONS_SUCCESS' => 'CONSUMPTION_WORKING'
        ];

        http_response_code(201);
        echo json_encode($result);

    } catch (PDOException $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}
?>
