<?php
// Debug test file to check Multi-Tenant functionality
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/middleware/auth.php';
require_once __DIR__ . '/utils/response.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

try {
    // Test authentication
    $user = requireAuth();
    
    // Test SQL query
    $stmt = $pdo->prepare("
        SELECT l.*,
               COALESCE(SUM(CASE WHEN b.menge > 0 THEN b.menge ELSE 0 END), 0) as quantity,
               sl.name as storage_location_name,
               sl.location_type,
               p.name as package_name,
               p.fill_amount,
               p.fill_unit,
               p.package_type
        FROM lebensmittel l
        LEFT JOIN lebensmittel_batches b ON l.id = b.lebensmittel_id
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        WHERE l.household_id = ? AND l.id = 27
        GROUP BY l.id
    ");
    $stmt->execute([$user['household_id']]);
    $item = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'debug' => 'Multi-Tenant Debug Test',
        'user' => $user,
        'raw_sql_result' => $item,
        'has_storage_location' => isset($item['storage_location_name']),
        'has_package' => isset($item['package_name']),
        'storage_location_id' => $item['storage_location_id'] ?? 'NULL',
        'package_id' => $item['package_id'] ?? 'NULL'
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'error' => $e->getMessage(),
        'trace' => $e->getTraceAsString()
    ]);
}
?>
