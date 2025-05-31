<?php
// Version check file
echo json_encode([
    'message' => 'Multi-Tenant Backend Version 2.0',
    'timestamp' => date('Y-m-d H:i:s'),
    'file_exists' => file_exists(__DIR__ . '/api/v1/lebensmittel.php'),
    'file_size' => file_exists(__DIR__ . '/api/v1/lebensmittel.php') ? filesize(__DIR__ . '/api/v1/lebensmittel.php') : 0,
    'file_modified' => file_exists(__DIR__ . '/api/v1/lebensmittel.php') ? date('Y-m-d H:i:s', filemtime(__DIR__ . '/api/v1/lebensmittel.php')) : 'not found'
]);
?>
