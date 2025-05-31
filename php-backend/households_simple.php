<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Einfacher Test ohne Datenbankverbindung erstmal
$token = $_GET['token'] ?? 'not provided';

if (!$token || $token === 'not provided') {
    echo json_encode([
        'error' => 'Token required in query parameter',
        'usage' => 'Add ?token=YOUR_TOKEN to URL',
        'received_get' => $_GET,
        'query_string' => $_SERVER['QUERY_STRING'] ?? 'empty'
    ]);
    exit;
}

// Token verifizieren
$decoded = base64_decode($token);
$parts = explode(':', $decoded);

if (count($parts) !== 2) {
    echo json_encode([
        'error' => 'Invalid token format',
        'token' => $token,
        'decoded' => $decoded,
        'parts' => $parts
    ]);
    exit;
}

$email = $parts[0];
$timestamp = (int)$parts[1];

// Token-Ablauf prüfen (24 Stunden)
if (time() - $timestamp > 86400) {
    echo json_encode([
        'error' => 'Token expired',
        'token_age_seconds' => time() - $timestamp,
        'max_age_seconds' => 86400
    ]);
    exit;
}

// Erfolg - Token ist gültig
echo json_encode([
    'success' => true,
    'message' => 'Token is valid',
    'user_email' => $email,
    'token_age_seconds' => time() - $timestamp,
    'households' => [
        [
            'id' => 1,
            'name' => 'Demo Haushalt',
            'description' => 'Standard Demo-Haushalt für FoodTrack',
            'member_count' => 1
        ]
    ],
    'timestamp' => date('Y-m-d H:i:s')
]);
?>
