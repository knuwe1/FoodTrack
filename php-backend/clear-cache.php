<?php
// Clear PHP OpCache and force reload
if (function_exists('opcache_reset')) {
    opcache_reset();
    $opcache_cleared = true;
} else {
    $opcache_cleared = false;
}

// Check current lebensmittel.php content
$file = __DIR__ . '/api/v1/lebensmittel.php';
$content = file_get_contents($file);
$size = filesize($file);
$has_multi_tenant = strpos($content, 'REPLACED_SUCCESS') !== false;
$has_storage_location = strpos($content, 'storage_location_name') !== false;

echo json_encode([
    'cache_clear' => 'executed',
    'opcache_cleared' => $opcache_cleared,
    'file_size' => $size,
    'has_multi_tenant_marker' => $has_multi_tenant,
    'has_storage_location_query' => $has_storage_location,
    'timestamp' => date('Y-m-d H:i:s'),
    'first_100_chars' => substr($content, 0, 100)
]);
?>
