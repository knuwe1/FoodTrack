<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Load configuration
$config = require_once 'config.php';

// Database connection
try {
    $dsn = "mysql:host={$config['database']['host']};dbname={$config['database']['name']};charset={$config['database']['charset']}";
    $pdo = new PDO($dsn, $config['database']['user'], $config['database']['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
    exit();
}

// Router
$request_uri = $_SERVER['REQUEST_URI'];
$request_method = $_SERVER['REQUEST_METHOD'];

// Remove query string and decode URL
$path = parse_url($request_uri, PHP_URL_PATH);
$path = urldecode($path);

// Remove base path if needed (adjust for your hosting structure)
$base_path = '/api/v1';
if (strpos($path, $base_path) === 0) {
    $path = substr($path, strlen($base_path));
}

// Route handling
switch (true) {
    // Users endpoints
    case preg_match('#^/users/login-json$#', $path):
        if ($request_method === 'POST') {
            require_once 'endpoints/users.php';
            login_user($pdo);
        }
        break;

    case preg_match('#^/users/?$#', $path):
        if ($request_method === 'POST') {
            require_once 'endpoints/users.php';
            create_user($pdo);
        }
        break;

    // Lebensmittel endpoints
    case preg_match('#^/lebensmittel/?$#', $path):
        require_once 'endpoints/lebensmittel.php';
        if ($request_method === 'GET') {
            get_lebensmittel_list($pdo);
        } elseif ($request_method === 'POST') {
            create_lebensmittel($pdo);
        }
        break;

    case preg_match('#^/lebensmittel/(\d+)$#', $path, $matches):
        require_once 'endpoints/lebensmittel.php';
        $id = (int)$matches[1];
        if ($request_method === 'GET') {
            get_lebensmittel($pdo, $id);
        } elseif ($request_method === 'PATCH') {
            update_lebensmittel($pdo, $id);
        } elseif ($request_method === 'DELETE') {
            delete_lebensmittel($pdo, $id);
        }
        break;

    case preg_match('#^/lebensmittel/ean/(.+)$#', $path, $matches):
        if ($request_method === 'GET') {
            require_once 'endpoints/lebensmittel.php';
            get_lebensmittel_by_ean($pdo, $matches[1]);
        }
        break;

    case preg_match('#^/lebensmittel/warnings/low-stock$#', $path):
        if ($request_method === 'GET') {
            require_once 'endpoints/lebensmittel.php';
            get_low_stock_items($pdo);
        }
        break;

    // Transaction endpoints
    case preg_match('#^/transactions/?$#', $path):
        require_once 'endpoints/transactions.php';
        if ($request_method === 'GET') {
            get_transactions($pdo);
        } elseif ($request_method === 'POST') {
            create_transaction($pdo);
        }
        break;

    // Convenience endpoints for purchase and consumption
    case preg_match('#^/transactions/purchase/(\d+)$#', $path, $matches):
        if ($request_method === 'POST') {
            require_once 'endpoints/transactions.php';
            record_purchase($pdo, (int)$matches[1]);
        }
        break;

    case preg_match('#^/transactions/consume/(\d+)$#', $path, $matches):
        if ($request_method === 'POST') {
            require_once 'endpoints/transactions.php';
            record_consumption($pdo, (int)$matches[1]);
        }
        break;

    default:
        http_response_code(404);
        echo json_encode(['error' => 'Endpoint not found']);
        break;
}
?>
