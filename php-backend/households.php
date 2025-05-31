<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

try {
    // Lade Config
    require_once __DIR__ . '/config.php';

    // Erstelle Datenbankverbindung
    $db_config = $config['database'];
    $dsn = "mysql:host={$db_config['host']};dbname={$db_config['name']};charset=utf8mb4";
    $pdo = new PDO($dsn, $db_config['user'], $db_config['pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);

    // Einfache Auth-Prüfung - Apache-kompatibel (alle Varianten)
    $headers = getallheaders();
    $authHeader = $headers['Authorization'] ??
                  $headers['authorization'] ??
                  $_SERVER['HTTP_AUTHORIZATION'] ??
                  $_ENV['HTTP_AUTHORIZATION'] ??
                  (function_exists('apache_request_headers') ? (apache_request_headers()['Authorization'] ?? null) : null) ??
                  null;

    // Debug-Info für Troubleshooting
    if (!$authHeader) {
        http_response_code(401);
        echo json_encode([
            'error' => 'Authorization header required',
            'debug' => [
                'available_headers' => array_keys($headers),
                'auth_variants' => [
                    'Authorization' => $headers['Authorization'] ?? 'not set',
                    'authorization' => $headers['authorization'] ?? 'not set',
                    'HTTP_AUTHORIZATION' => $_SERVER['HTTP_AUTHORIZATION'] ?? 'not set'
                ]
            ]
        ]);
        exit;
    }

    if (!preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
        http_response_code(401);
        echo json_encode([
            'error' => 'Invalid authorization format',
            'received_header' => $authHeader
        ]);
        exit;
    }

    $token = $matches[1];

    // Token verifizieren (vereinfacht)
    $decoded = base64_decode($token);
    $parts = explode(':', $decoded);

    if (count($parts) !== 2) {
        http_response_code(401);
        echo json_encode(['error' => 'Invalid token format']);
        exit;
    }

    $email = $parts[0];
    $timestamp = (int)$parts[1];

    // Token-Ablauf prüfen (24 Stunden)
    if (time() - $timestamp > 86400) {
        http_response_code(401);
        echo json_encode(['error' => 'Token expired']);
        exit;
    }

    // Benutzer laden
    $stmt = $pdo->prepare("SELECT id, email, household_id FROM users WHERE email = ? AND is_active = 1");
    $stmt->execute([$email]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => 'User not found']);
        exit;
    }

    // Haushalte für den Benutzer laden
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $stmt = $pdo->prepare("
            SELECT h.*,
                   (SELECT COUNT(*) FROM users WHERE household_id = h.id AND is_active = 1) as member_count
            FROM households h
            WHERE h.id = ? AND h.is_active = 1
        ");
        $stmt->execute([$user['household_id']]);
        $households = $stmt->fetchAll(PDO::FETCH_ASSOC);

        http_response_code(200);
        echo json_encode([
            'households' => $households,
            'user_info' => [
                'id' => $user['id'],
                'email' => $user['email'],
                'household_id' => $user['household_id']
            ],
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        http_response_code(405);
        echo json_encode(['error' => 'Method not allowed']);
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'error' => 'Server error: ' . $e->getMessage(),
        'file' => $e->getFile(),
        'line' => $e->getLine()
    ]);
}
?>
