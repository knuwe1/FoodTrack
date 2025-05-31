<?php
// php-backend/api/v1/households.php

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

// Require authentication for all household operations
$user = requireAuth();

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$pathParts = explode('/', trim($path, '/'));

try {
    switch ($method) {
        case 'GET':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // GET /api/v1/households/{id}
                getHouseholdById($pathParts[4], $user);
            } elseif (isset($pathParts[4]) && $pathParts[4] === 'members' && isset($pathParts[3])) {
                // GET /api/v1/households/{id}/members
                getHouseholdMembers($pathParts[3], $user);
            } else {
                // GET /api/v1/households/
                getMyHouseholds($user);
            }
            break;

        case 'POST':
            if (isset($pathParts[4]) && $pathParts[4] === 'invite' && isset($pathParts[3])) {
                // POST /api/v1/households/{id}/invite
                generateInviteCode($pathParts[3], $user);
            } elseif (isset($pathParts[3]) && $pathParts[3] === 'join') {
                // POST /api/v1/households/join
                joinHousehold($user);
            } else {
                // POST /api/v1/households/
                createHousehold($user);
            }
            break;

        case 'PATCH':
            if (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // PATCH /api/v1/households/{id}
                updateHousehold($pathParts[4], $user);
            }
            break;

        case 'DELETE':
            if (isset($pathParts[6]) && isset($pathParts[4]) && $pathParts[4] === 'members') {
                // DELETE /api/v1/households/{household_id}/members/{user_id}
                removeHouseholdMember($pathParts[3], $pathParts[6], $user);
            } elseif (isset($pathParts[4]) && is_numeric($pathParts[4])) {
                // DELETE /api/v1/households/{id}
                deleteHousehold($pathParts[4], $user);
            }
            break;

        default:
            sendError('Method not allowed', 405);
    }
} catch (Exception $e) {
    error_log("Household API Error: " . $e->getMessage());
    sendError('Internal server error: ' . $e->getMessage(), 500);
}

function getMyHouseholds($user) {
    global $pdo;

    // If user has no household_id, return empty array
    if (!isset($user['household_id']) || $user['household_id'] === null) {
        sendSuccess([]);
        return;
    }

    $stmt = $pdo->prepare("
        SELECT h.*,
               (SELECT COUNT(*) FROM users WHERE household_id = h.id AND is_active = 1) as member_count
        FROM households h
        WHERE h.id = ? AND h.is_active = 1
    ");
    $stmt->execute([$user['household_id']]);
    $households = $stmt->fetchAll(PDO::FETCH_ASSOC);

    sendSuccess($households);
}

function getHouseholdById($householdId, $user) {
    global $pdo;

    // Check if user belongs to this household
    if ($user['household_id'] != $householdId) {
        sendError('Access denied', 403);
    }

    $stmt = $pdo->prepare("
        SELECT h.*,
               u.display_name as admin_name,
               (SELECT COUNT(*) FROM users WHERE household_id = h.id AND is_active = 1) as member_count
        FROM households h
        LEFT JOIN users u ON h.admin_user_id = u.id
        WHERE h.id = ? AND h.is_active = 1
    ");
    $stmt->execute([$householdId]);
    $household = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$household) {
        sendError('Household not found', 404);
    }

    sendSuccess($household);
}

function createHousehold($user) {
    global $pdo;

    $input = json_decode(file_get_contents('php://input'), true);

    if (!isset($input['name']) || empty(trim($input['name']))) {
        sendError('Household name is required', 400);
    }

    $pdo->beginTransaction();

    try {
        // Create household
        $stmt = $pdo->prepare("
            INSERT INTO households (name, description, admin_user_id, is_active)
            VALUES (?, ?, ?, 1)
        ");
        $stmt->execute([
            trim($input['name']),
            $input['description'] ?? null,
            $user['id']
        ]);

        $householdId = $pdo->lastInsertId();

        // Update user's household_id and make them admin
        $stmt = $pdo->prepare("
            UPDATE users
            SET household_id = ?, role = 'admin'
            WHERE id = ?
        ");
        $stmt->execute([$householdId, $user['id']]);

        $pdo->commit();

        // Return created household
        getHouseholdById($householdId, array_merge($user, ['household_id' => $householdId]));

    } catch (Exception $e) {
        $pdo->rollBack();
        throw $e;
    }
}

function generateInviteCode($householdId, $user) {
    global $pdo;

    // Check if user is admin of this household
    if ($user['household_id'] != $householdId || $user['role'] !== 'admin') {
        sendError('Access denied', 403);
    }

    $inviteCode = bin2hex(random_bytes(16));
    $expiresAt = date('Y-m-d H:i:s', strtotime('+7 days'));

    $stmt = $pdo->prepare("
        UPDATE households
        SET invite_code = ?, invite_expires_at = ?
        WHERE id = ?
    ");
    $stmt->execute([$inviteCode, $expiresAt, $householdId]);

    sendSuccess([
        'invite_code' => $inviteCode,
        'expires_at' => $expiresAt
    ]);
}

function joinHousehold($user) {
    global $pdo;

    $input = json_decode(file_get_contents('php://input'), true);

    if (!isset($input['invite_code']) || empty($input['invite_code'])) {
        sendError('Invite code is required', 400);
    }

    $stmt = $pdo->prepare("
        SELECT * FROM households
        WHERE invite_code = ?
        AND invite_expires_at > NOW()
        AND is_active = 1
    ");
    $stmt->execute([$input['invite_code']]);
    $household = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$household) {
        sendError('Invalid or expired invite code', 400);
    }

    // Update user's household
    $stmt = $pdo->prepare("
        UPDATE users
        SET household_id = ?, role = 'member'
        WHERE id = ?
    ");
    $stmt->execute([$household['id'], $user['id']]);

    sendSuccess($household);
}

function getHouseholdMembers($householdId, $user) {
    global $pdo;

    // Check if user belongs to this household
    if ($user['household_id'] != $householdId) {
        sendError('Access denied', 403);
    }

    $stmt = $pdo->prepare("
        SELECT id, username, email, display_name, role, is_active, created_at, last_login_at
        FROM users
        WHERE household_id = ? AND is_active = 1
        ORDER BY role DESC, display_name ASC
    ");
    $stmt->execute([$householdId]);
    $members = $stmt->fetchAll(PDO::FETCH_ASSOC);

    sendSuccess($members);
}

function removeHouseholdMember($householdId, $userId, $user) {
    global $pdo;

    // Check if user is admin of this household
    if ($user['household_id'] != $householdId || $user['role'] !== 'admin') {
        sendError('Access denied', 403);
    }

    // Cannot remove yourself
    if ($user['id'] == $userId) {
        sendError('Cannot remove yourself', 400);
    }

    $stmt = $pdo->prepare("
        UPDATE users
        SET is_active = 0
        WHERE id = ? AND household_id = ?
    ");
    $stmt->execute([$userId, $householdId]);

    if ($stmt->rowCount() === 0) {
        sendError('User not found', 404);
    }

    sendSuccess(['message' => 'User removed successfully']);
}

function updateHousehold($householdId, $user) {
    global $pdo;

    // Check if user is admin of this household
    if ($user['household_id'] != $householdId || $user['role'] !== 'admin') {
        sendError('Access denied', 403);
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

    if (isset($input['is_active'])) {
        $updates[] = "is_active = ?";
        $params[] = $input['is_active'] ? 1 : 0;
    }

    if (empty($updates)) {
        sendError('No valid fields to update', 400);
    }

    $params[] = $householdId;

    $stmt = $pdo->prepare("
        UPDATE households
        SET " . implode(', ', $updates) . ", updated_at = NOW()
        WHERE id = ?
    ");
    $stmt->execute($params);

    getHouseholdById($householdId, $user);
}

function deleteHousehold($householdId, $user) {
    global $pdo;

    // Check if user is admin of this household
    if ($user['household_id'] != $householdId || $user['role'] !== 'admin') {
        sendError('Access denied', 403);
    }

    $stmt = $pdo->prepare("
        UPDATE households
        SET is_active = 0
        WHERE id = ?
    ");
    $stmt->execute([$householdId]);

    sendSuccess(['message' => 'Household deleted successfully']);
}
?>
