<?php
header('Content-Type: application/json');

echo json_encode([
    'message' => 'Simple token test',
    'get_params' => $_GET,
    'token_from_get' => $_GET['token'] ?? 'not provided',
    'request_method' => $_SERVER['REQUEST_METHOD'],
    'request_uri' => $_SERVER['REQUEST_URI'],
    'query_string' => $_SERVER['QUERY_STRING'] ?? 'empty',
    'timestamp' => date('Y-m-d H:i:s')
]);
?>
