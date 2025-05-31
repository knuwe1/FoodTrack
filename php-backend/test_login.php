<?php
// Temporäre Login-Test-Datei
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    if (!isset($input['username']) || !isset($input['password'])) {
        http_response_code(400);
        echo json_encode(['error' => 'Username and password required']);
        exit();
    }
    
    $username = $input['username'];
    $password = $input['password'];
    
    try {
        // Find user by email
        $stmt = $pdo->prepare("SELECT id, email, password_hash, is_active FROM users WHERE email = ?");
        $stmt->execute([$username]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$user) {
            echo json_encode([
                'error' => 'User not found',
                'debug' => 'No user with email: ' . $username
            ]);
            exit();
        }
        
        if (!password_verify($password, $user['password_hash'])) {
            echo json_encode([
                'error' => 'Invalid password',
                'debug' => [
                    'provided_password' => $password,
                    'stored_hash' => $user['password_hash'],
                    'verification_result' => password_verify($password, $user['password_hash'])
                ]
            ]);
            exit();
        }
        
        if (!$user['is_active']) {
            echo json_encode(['error' => 'Account is inactive']);
            exit();
        }
        
        // Generate token
        $token = base64_encode($user['email'] . ':' . time());
        
        echo json_encode([
            'access_token' => $token,
            'token_type' => 'bearer',
            'user' => [
                'id' => $user['id'],
                'email' => $user['email']
            ]
        ]);
        
    } catch (PDOException $e) {
        echo json_encode([
            'error' => 'Database error',
            'debug' => $e->getMessage()
        ]);
    }
} else {
    echo json_encode(['error' => 'Only POST method allowed']);
}
?>
