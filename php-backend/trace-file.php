<?php
// Trace which file is actually being loaded
echo json_encode([
    'trace' => 'File path investigation',
    'timestamp' => date('Y-m-d H:i:s'),
    'files_found' => [
        'api_v1_lebensmittel' => [
            'exists' => file_exists(__DIR__ . '/api/v1/lebensmittel.php'),
            'size' => file_exists(__DIR__ . '/api/v1/lebensmittel.php') ? filesize(__DIR__ . '/api/v1/lebensmittel.php') : 0,
            'modified' => file_exists(__DIR__ . '/api/v1/lebensmittel.php') ? date('Y-m-d H:i:s', filemtime(__DIR__ . '/api/v1/lebensmittel.php')) : 'not found',
            'first_line' => file_exists(__DIR__ . '/api/v1/lebensmittel.php') ? trim(explode("\n", file_get_contents(__DIR__ . '/api/v1/lebensmittel.php'))[0]) : 'not found'
        ],
        'endpoints_lebensmittel' => [
            'exists' => file_exists(__DIR__ . '/endpoints/lebensmittel.php'),
            'size' => file_exists(__DIR__ . '/endpoints/lebensmittel.php') ? filesize(__DIR__ . '/endpoints/lebensmittel.php') : 0,
            'modified' => file_exists(__DIR__ . '/endpoints/lebensmittel.php') ? date('Y-m-d H:i:s', filemtime(__DIR__ . '/endpoints/lebensmittel.php')) : 'not found',
            'first_line' => file_exists(__DIR__ . '/endpoints/lebensmittel.php') ? trim(explode("\n", file_get_contents(__DIR__ . '/endpoints/lebensmittel.php'))[0]) : 'not found'
        ],
        'root_lebensmittel' => [
            'exists' => file_exists(__DIR__ . '/lebensmittel.php'),
            'size' => file_exists(__DIR__ . '/lebensmittel.php') ? filesize(__DIR__ . '/lebensmittel.php') : 0,
            'modified' => file_exists(__DIR__ . '/lebensmittel.php') ? date('Y-m-d H:i:s', filemtime(__DIR__ . '/lebensmittel.php')) : 'not found',
            'first_line' => file_exists(__DIR__ . '/lebensmittel.php') ? trim(explode("\n", file_get_contents(__DIR__ . '/lebensmittel.php'))[0]) : 'not found'
        ]
    ],
    'htaccess_check' => [
        'api_htaccess_exists' => file_exists(__DIR__ . '/api/.htaccess'),
        'api_v1_htaccess_exists' => file_exists(__DIR__ . '/api/v1/.htaccess'),
        'root_htaccess_exists' => file_exists(__DIR__ . '/.htaccess'),
        'root_htaccess_content' => file_exists(__DIR__ . '/.htaccess') ? substr(file_get_contents(__DIR__ . '/.htaccess'), 0, 200) : 'not found'
    ]
]);
?>
