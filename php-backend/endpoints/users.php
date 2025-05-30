<?php

function create_user($pdo) {
    $input = json_decode(file_get_contents('php://input'), true);
    
    if (!isset($input['email']) || !isset($input['password'])) {
        http_response_code(400);
        echo json_encode(['error' => 'Email and password required']);
        return;
    }
    
    $email = filter_var($input['email'], FILTER_VALIDATE_EMAIL);
    if (!$email) {
        http_response_code(400);
        echo json_encode(['error' => 'Invalid email format']);
        return;
    }
    
    $password = $input['password'];
    if (strlen($password) < 3) {
        http_response_code(400);
        echo json_encode(['error' => 'Password must be at least 3 characters']);
        return;
    }
    
    try {
        // Check if user already exists
        $stmt = $pdo->prepare("SELECT id FROM users WHERE email = ?");
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            http_response_code(409);
            echo json_encode(['error' => 'User already exists']);
            return;
        }
        
        // Create user
        $password_hash = password_hash($password, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare("INSERT INTO users (email, password_hash) VALUES (?, ?)");
        $stmt->execute([$email, $password_hash]);
        
        $user_id = $pdo->lastInsertId();
        
        http_response_code(201);
        echo json_encode([
            'id' => (int)$user_id,
            'email' => $email,
            'is_active' => true
        ]);
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function login_user($pdo) {
    $input = json_decode(file_get_contents('php://input'), true);
    
    if (!isset($input['username']) || !isset($input['password'])) {
        http_response_code(400);
        echo json_encode(['error' => 'Username and password required']);
        return;
    }
    
    $username = $input['username']; // Can be email
    $password = $input['password'];
    
    try {
        // Find user by email
        $stmt = $pdo->prepare("SELECT id, email, password_hash, is_active FROM users WHERE email = ?");
        $stmt->execute([$username]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$user || !password_verify($password, $user['password_hash'])) {
            http_response_code(401);
            echo json_encode(['error' => 'Invalid credentials']);
            return;
        }
        
        if (!$user['is_active']) {
            http_response_code(401);
            echo json_encode(['error' => 'Account is inactive']);
            return;
        }
        
        // Generate simple token (in production, use JWT)
        $token = base64_encode($user['email'] . ':' . time());
        
        http_response_code(200);
        echo json_encode([
            'access_token' => $token,
            'token_type' => 'bearer'
        ]);
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    }
}

function verify_token($token) {
    // Simple token verification (in production, use proper JWT)
    $decoded = base64_decode($token);
    $parts = explode(':', $decoded);
    
    if (count($parts) !== 2) {
        return false;
    }
    
    $email = $parts[0];
    $timestamp = (int)$parts[1];
    
    // Token expires after 24 hours
    if (time() - $timestamp > 86400) {
        return false;
    }
    
    return $email;
}

function require_auth() {
    $headers = getallheaders();
    $auth_header = $headers['Authorization'] ?? '';
    
    if (!preg_match('/Bearer\s+(.*)$/i', $auth_header, $matches)) {
        http_response_code(401);
        echo json_encode(['error' => 'Authorization header required']);
        exit();
    }
    
    $token = $matches[1];
    $email = verify_token($token);
    
    if (!$email) {
        http_response_code(401);
        echo json_encode(['error' => 'Invalid or expired token']);
        exit();
    }
    
    return $email;
}

?>
