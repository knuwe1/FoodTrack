<?php
// Simple debug test without authentication
require_once __DIR__ . '/config/database.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

try {
    // Test database connection
    $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM lebensmittel");
    $stmt->execute();
    $count = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Test specific item with Multi-Tenant fields
    $stmt = $pdo->prepare("
        SELECT l.*,
               sl.name as storage_location_name,
               sl.location_type,
               p.name as package_name,
               p.fill_amount,
               p.fill_unit,
               p.package_type
        FROM lebensmittel l
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        WHERE l.id = 27
    ");
    $stmt->execute();
    $item = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Test if columns exist
    $stmt = $pdo->prepare("DESCRIBE lebensmittel");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'debug' => 'Simple Multi-Tenant Debug Test',
        'total_lebensmittel' => $count['count'],
        'item_27' => $item,
        'has_storage_location_join' => isset($item['storage_location_name']),
        'has_package_join' => isset($item['package_name']),
        'lebensmittel_columns' => array_column($columns, 'Field'),
        'storage_location_id_in_item' => $item['storage_location_id'] ?? 'NULL',
        'package_id_in_item' => $item['package_id'] ?? 'NULL'
    ], JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    echo json_encode([
        'error' => $e->getMessage(),
        'trace' => $e->getTraceAsString()
    ], JSON_PRETTY_PRINT);
}
?>
