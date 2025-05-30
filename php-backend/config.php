<?php
// FoodTrack PHP Backend Configuration

function loadEnvFile($path) {
    if (!file_exists($path)) {
        return false;
    }

    $lines = file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        // Skip comments
        if (strpos(trim($line), '#') === 0) {
            continue;
        }

        // Parse KEY=VALUE
        if (strpos($line, '=') !== false) {
            list($key, $value) = explode('=', $line, 2);
            $key = trim($key);
            $value = trim($value);

            // Remove quotes if present
            if ((substr($value, 0, 1) === '"' && substr($value, -1) === '"') ||
                (substr($value, 0, 1) === "'" && substr($value, -1) === "'")) {
                $value = substr($value, 1, -1);
            }

            // Set environment variable
            putenv("$key=$value");
            $_ENV[$key] = $value;
        }
    }

    return true;
}

function loadConfig() {
    // Default configuration
    $config = [
        'database' => [
            'host' => 'localhost',
            'name' => '',
            'user' => '',
            'pass' => '',
            'charset' => 'utf8mb4'
        ],

        'app' => [
            'name' => 'FoodTrack API',
            'version' => '1.0.0',
            'debug' => false,
            'timezone' => 'Europe/Berlin'
        ],

        'security' => [
            'token_expiry' => 86400,  // 24 hours in seconds
            'password_min_length' => 3,
            'jwt_secret' => 'default-secret-change-this'
        ]
    ];

    // 1. Try to load from .env file (primary method)
    if (loadEnvFile(__DIR__ . '/.env')) {
        $config['database']['host'] = getenv('DB_HOST') ?: $config['database']['host'];
        $config['database']['name'] = getenv('DB_NAME') ?: '';
        $config['database']['user'] = getenv('DB_USER') ?: '';
        $config['database']['pass'] = getenv('DB_PASS') ?: '';
        $config['security']['jwt_secret'] = getenv('JWT_SECRET') ?: $config['security']['jwt_secret'];
        $config['app']['debug'] = getenv('APP_DEBUG') === 'true';
    }
    // 2. Fallback to system environment variables
    elseif (getenv('DB_HOST')) {
        $config['database']['host'] = getenv('DB_HOST');
        $config['database']['name'] = getenv('DB_NAME');
        $config['database']['user'] = getenv('DB_USER');
        $config['database']['pass'] = getenv('DB_PASS');
        $config['security']['jwt_secret'] = getenv('JWT_SECRET') ?: $config['security']['jwt_secret'];
        $config['app']['debug'] = getenv('APP_DEBUG') === 'true';
    }
    // 3. Fallback to local config file
    elseif (file_exists(__DIR__ . '/config.local.php')) {
        $localConfig = include __DIR__ . '/config.local.php';
        $config = array_merge_recursive($config, $localConfig);
    }

    // Validate required configuration
    if (empty($config['database']['name']) || empty($config['database']['user'])) {
        throw new Exception('Database configuration missing. Please create .env file with DB_HOST, DB_NAME, DB_USER, and DB_PASS');
    }

    return $config;
}

// Load configuration
try {
    $config = loadConfig();
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Configuration error: ' . $e->getMessage()]);
    exit();
}

// Set timezone
date_default_timezone_set($config['app']['timezone']);

// Error reporting
if ($config['app']['debug']) {
    error_reporting(E_ALL);
    ini_set('display_errors', 1);
} else {
    error_reporting(0);
    ini_set('display_errors', 0);
}

return $config;
?>
