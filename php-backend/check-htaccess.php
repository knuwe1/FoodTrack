<?php
// Check .htaccess files
echo json_encode([
    'htaccess_investigation' => date('Y-m-d H:i:s'),
    'root_htaccess' => [
        'exists' => file_exists(__DIR__ . '/.htaccess'),
        'content' => file_exists(__DIR__ . '/.htaccess') ? file_get_contents(__DIR__ . '/.htaccess') : 'not found'
    ],
    'api_htaccess' => [
        'exists' => file_exists(__DIR__ . '/api/.htaccess'),
        'content' => file_exists(__DIR__ . '/api/.htaccess') ? file_get_contents(__DIR__ . '/api/.htaccess') : 'not found'
    ],
    'api_v1_htaccess' => [
        'exists' => file_exists(__DIR__ . '/api/v1/.htaccess'),
        'content' => file_exists(__DIR__ . '/api/v1/.htaccess') ? file_get_contents(__DIR__ . '/api/v1/.htaccess') : 'not found'
    ],
    'endpoints_lebensmittel' => [
        'exists' => file_exists(__DIR__ . '/endpoints/lebensmittel.php'),
        'size' => file_exists(__DIR__ . '/endpoints/lebensmittel.php') ? filesize(__DIR__ . '/endpoints/lebensmittel.php') : 0
    ]
], JSON_PRETTY_PRINT);
?>
