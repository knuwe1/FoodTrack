<?php
// System test after .htaccess restore
echo json_encode([
    'system_test' => 'SUCCESS',
    'timestamp' => date('Y-m-d H:i:s'),
    'php_version' => phpversion(),
    'server_info' => $_SERVER['SERVER_SOFTWARE'] ?? 'unknown'
]);
?>
