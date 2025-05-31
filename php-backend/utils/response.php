<?php
// php-backend/utils/response.php

function sendSuccess($data = null, $message = null, $statusCode = 200) {
    http_response_code($statusCode);
    
    $response = [];
    
    if ($message) {
        $response['message'] = $message;
    }
    
    if ($data !== null) {
        $response['data'] = $data;
    }
    
    // If only data is provided, send it directly for backward compatibility
    if ($data !== null && !$message) {
        echo json_encode($data);
    } else {
        echo json_encode($response);
    }
    exit();
}

function sendError($message, $statusCode = 400, $details = null) {
    http_response_code($statusCode);
    
    $response = [
        'error' => $message
    ];
    
    if ($details) {
        $response['details'] = $details;
    }
    
    echo json_encode($response);
    exit();
}

function sendValidationError($errors, $message = 'Validation failed') {
    sendError($message, 422, $errors);
}

function sendNotFound($message = 'Resource not found') {
    sendError($message, 404);
}

function sendUnauthorized($message = 'Unauthorized') {
    sendError($message, 401);
}

function sendForbidden($message = 'Access denied') {
    sendError($message, 403);
}

function sendInternalError($message = 'Internal server error') {
    sendError($message, 500);
}
?>
