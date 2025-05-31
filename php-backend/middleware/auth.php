<?php
// php-backend/middleware/auth.php - Multi-Tenant Version with Real Auth

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../utils/response.php';
require_once __DIR__ . '/../endpoints/users.php';

function requireAuth() {
    global $pdo, $config;

    try {
        // Erstelle Datenbankverbindung falls nicht vorhanden
        if (!isset($pdo)) {
            if (!isset($config)) {
                http_response_code(500);
                echo json_encode(['error' => 'Config not loaded']);
                exit;
            }

            $db_config = $config['database'];
            $dsn = "mysql:host={$db_config['host']};dbname={$db_config['name']};charset=utf8mb4";
            $pdo = new PDO($dsn, $db_config['user'], $db_config['pass'], [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
            ]);
        }
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
        exit;
    }

    $headers = getallheaders();
    $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;

    if (!$authHeader || !preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
        sendError('Authorization header required', 401);
    }

    $token = $matches[1];

    // Use the existing token verification system
    $email = verify_token($token);

    if (!$email) {
        sendError('Invalid or expired token', 401);
    }

    // Get user with household information
    $stmt = $pdo->prepare("
        SELECT u.*, h.name as household_name, h.admin_user_id
        FROM users u
        LEFT JOIN households h ON u.household_id = h.id
        WHERE u.email = ? AND u.is_active = 1
    ");
    $stmt->execute([$email]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        sendError('User not found or inactive', 401);
    }

    // If user doesn't have a household yet, assign to default household
    if (!$user['household_id']) {
        // Create or get default household
        $stmt = $pdo->prepare("SELECT id FROM households WHERE name = 'Demo Haushalt' LIMIT 1");
        $stmt->execute();
        $household = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$household) {
            // Create default household
            $stmt = $pdo->prepare("INSERT INTO households (name, description, admin_user_id, is_active) VALUES (?, ?, ?, 1)");
            $stmt->execute(['Demo Haushalt', 'Standard Demo-Haushalt für FoodTrack', $user['id']]);
            $householdId = $pdo->lastInsertId();
        } else {
            $householdId = $household['id'];
        }

        // Update user with household
        $stmt = $pdo->prepare("UPDATE users SET household_id = ?, username = ?, display_name = ?, role = 'admin' WHERE id = ?");
        $username = explode('@', $user['email'])[0];
        $stmt->execute([$householdId, $username, $username, $user['id']]);

        // Refresh user data
        $stmt = $pdo->prepare("
            SELECT u.*, h.name as household_name, h.admin_user_id
            FROM users u
            LEFT JOIN households h ON u.household_id = h.id
            WHERE u.email = ? AND u.is_active = 1
        ");
        $stmt->execute([$email]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
    }

    // Update last login
    $stmt = $pdo->prepare("UPDATE users SET last_login_at = NOW() WHERE id = ?");
    $stmt->execute([$user['id']]);

    return [
        'id' => (int)$user['id'],
        'username' => $user['username'] ?: explode('@', $user['email'])[0],
        'email' => $user['email'],
        'display_name' => $user['display_name'] ?: explode('@', $user['email'])[0],
        'household_id' => (int)$user['household_id'],
        'household_name' => $user['household_name'],
        'role' => $user['role'] ?: 'admin',
        'is_household_admin' => $user['admin_user_id'] == $user['id'],
        'is_active' => (bool)$user['is_active']
    ];
}

function requireHouseholdAdmin() {
    $user = requireAuth();

    if ($user['role'] !== 'admin') {
        sendError('Access denied. Household admin privileges required.', 403);
    }

    return $user;
}

function validateHouseholdAccess($householdId, $user) {
    if ($user['household_id'] != $householdId) {
        sendError('Access denied. You do not belong to this household.', 403);
    }
}
?>
