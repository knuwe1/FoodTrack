<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Pfad zur FoodTrack-Konfiguration
require_once 'foodtrack/config/database.php';

$email = 'admin@foodtrack.com';
$password = 'admin';

try {
    // Prüfe Datenbankverbindung
    $debug = [
        'database_connection' => 'OK',
        'timestamp' => date('Y-m-d H:i:s')
    ];
    
    // Prüfe Benutzer
    $stmt = $pdo->prepare("SELECT id, username, email, password_hash, is_active, household_id FROM users WHERE email = ?");
    $stmt->execute([$email]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        $debug['error'] = 'User not found';
        $debug['email_searched'] = $email;
        
        // Zeige alle Benutzer
        $stmt = $pdo->prepare("SELECT id, email, is_active FROM users LIMIT 5");
        $stmt->execute();
        $debug['all_users'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    } else {
        $debug['user_found'] = true;
        $debug['user_data'] = [
            'id' => $user['id'],
            'username' => $user['username'],
            'email' => $user['email'],
            'is_active' => $user['is_active'],
            'household_id' => $user['household_id']
        ];
        
        // Teste Passwort-Verifikation
        $debug['password_test'] = [
            'provided_password' => $password,
            'stored_hash' => $user['password_hash'],
            'verification_result' => password_verify($password, $user['password_hash'])
        ];
        
        // Generiere neuen Hash zum Vergleich
        $new_hash = password_hash($password, PASSWORD_DEFAULT);
        $debug['new_hash_test'] = [
            'new_hash' => $new_hash,
            'new_hash_verify' => password_verify($password, $new_hash)
        ];
        
        // Teste bekannte Hashes
        $known_hashes = [
            'hash1' => '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
            'hash2' => '$2y$10$TKh8H1.PfQx37YgCzwiKb.KjNyWgaHb9cbcoQgdIVFlYg7B77UdFm'
        ];
        
        foreach ($known_hashes as $name => $hash) {
            $debug['known_hashes'][$name] = [
                'hash' => $hash,
                'verify' => password_verify($password, $hash)
            ];
        }
    }
    
    // Prüfe Tabellen-Status
    $tables = ['users', 'households', 'packages', 'storage_locations', 'lebensmittel'];
    foreach ($tables as $table) {
        try {
            $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM $table");
            $stmt->execute();
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
            $debug['table_counts'][$table] = $result['count'];
        } catch (Exception $e) {
            $debug['table_counts'][$table] = 'ERROR: ' . $e->getMessage();
        }
    }
    
    echo json_encode($debug, JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    echo json_encode([
        'error' => 'Database connection failed',
        'message' => $e->getMessage(),
        'file' => $e->getFile(),
        'line' => $e->getLine()
    ], JSON_PRETTY_PRINT);
}
?>