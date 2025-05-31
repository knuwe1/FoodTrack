<?php
// Debug routing - add this to index.php to see what's happening
$debug_info = [
    'debug_routing' => 'ACTIVE',
    'timestamp' => date('Y-m-d H:i:s'),
    'request_uri' => $_SERVER['REQUEST_URI'] ?? 'not set',
    'request_method' => $_SERVER['REQUEST_METHOD'] ?? 'not set',
    'script_name' => $_SERVER['SCRIPT_NAME'] ?? 'not set',
    'php_self' => $_SERVER['PHP_SELF'] ?? 'not set',
    'current_file' => __FILE__,
    'current_dir' => __DIR__,
    'path_info' => $_SERVER['PATH_INFO'] ?? 'not set',
    'query_string' => $_SERVER['QUERY_STRING'] ?? 'not set'
];

// Log to file
file_put_contents(__DIR__ . '/debug.log', json_encode($debug_info) . "\n", FILE_APPEND);

// Also output
echo json_encode($debug_info, JSON_PRETTY_PRINT);
?>
