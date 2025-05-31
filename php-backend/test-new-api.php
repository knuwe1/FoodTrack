<?php
// Test the new API directly
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/middleware/auth.php';
require_once __DIR__ . '/utils/response.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

try {
    $user = requireAuth();
    
    // Test the new Multi-Tenant query
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
        WHERE l.household_id = ?
        GROUP BY l.id
        ORDER BY l.name
        LIMIT 1
    ");
    $stmt->execute([$user['household_id']]);
    $item = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($item) {
        $result = [
            'SUCCESS' => 'NEW MULTI-TENANT API WORKS!',
            'item' => [
                'id' => (int)$item['id'],
                'name' => $item['name'],
                'quantity' => (int)$item['quantity'],
                'einheit' => $item['einheit'],
                'kategorie' => $item['kategorie'],
                'storage_location_id' => $item['storage_location_id'] ? (int)$item['storage_location_id'] : null,
                'storage_location_name' => $item['storage_location_name'],
                'location_type' => $item['location_type'],
                'package_id' => $item['package_id'] ? (int)$item['package_id'] : null,
                'package_name' => $item['package_name'],
                'package_count' => (int)$item['package_count'],
                'package_fill_amount' => $item['fill_amount'] ? (float)$item['fill_amount'] : null,
                'package_fill_unit' => $item['fill_unit'],
                'package_type' => $item['package_type']
            ],
            'has_storage_location' => !empty($item['storage_location_name']),
            'has_package' => !empty($item['package_name']),
            'timestamp' => date('Y-m-d H:i:s')
        ];
    } else {
        $result = [
            'SUCCESS' => 'NEW MULTI-TENANT API WORKS!',
            'message' => 'No items found',
            'timestamp' => date('Y-m-d H:i:s')
        ];
    }
    
    echo json_encode($result, JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    echo json_encode([
        'ERROR' => $e->getMessage(),
        'trace' => $e->getTraceAsString()
    ], JSON_PRETTY_PRINT);
}
?>
