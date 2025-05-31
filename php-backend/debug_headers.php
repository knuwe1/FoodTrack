<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$all_headers = getallheaders();
$server_vars = $_SERVER;

// Filter relevante Server-Variablen
$relevant_server = [];
foreach ($server_vars as $key => $value) {
    if (strpos($key, 'HTTP_') === 0 || in_array($key, ['REQUEST_METHOD', 'REQUEST_URI', 'CONTENT_TYPE'])) {
        $relevant_server[$key] = $value;
    }
}

echo json_encode([
    'message' => 'Header debug information',
    'all_headers' => $all_headers,
    'relevant_server_vars' => $relevant_server,
    'authorization_variants' => [
        'Authorization' => $all_headers['Authorization'] ?? 'NOT SET',
        'authorization' => $all_headers['authorization'] ?? 'NOT SET',
        'HTTP_AUTHORIZATION' => $_SERVER['HTTP_AUTHORIZATION'] ?? 'NOT SET'
    ],
    'timestamp' => date('Y-m-d H:i:s')
], JSON_PRETTY_PRINT);
?>
