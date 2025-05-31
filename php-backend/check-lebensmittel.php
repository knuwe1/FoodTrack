<?php
$file_path = __DIR__ . '/api/v1/lebensmittel.php';

echo json_encode([
    'file_exists' => file_exists($file_path),
    'file_size' => file_exists($file_path) ? filesize($file_path) : 0,
    'file_modified' => file_exists($file_path) ? date('Y-m-d H:i:s', filemtime($file_path)) : 'not found',
    'first_100_chars' => file_exists($file_path) ? substr(file_get_contents($file_path), 0, 100) : 'not found',
    'contains_debug' => file_exists($file_path) ? (strpos(file_get_contents($file_path), 'DEBUG_VERSION') !== false) : false,
    'contains_multi_tenant' => file_exists($file_path) ? (strpos(file_get_contents($file_path), 'Multi-Tenant Version') !== false) : false
]);
?>
