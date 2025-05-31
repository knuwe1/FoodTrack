<?php
// Direct database check for Multi-Tenant data
$config = [
    'host' => 'mysql37.1blu.de',
    'name' => 'db6632x3717241',
    'user' => 's6632_3717241',
    'pass' => 'FoodTrack2024!'
];

try {
    $pdo = new PDO(
        "mysql:host={$config['host']};dbname={$config['name']};charset=utf8mb4",
        $config['user'],
        $config['pass'],
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false
        ]
    );
} catch (PDOException $e) {
    die(json_encode(['ERROR' => 'Database connection failed: ' . $e->getMessage()]));
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

try {
    // Check lebensmittel table structure
    $stmt = $pdo->prepare("DESCRIBE lebensmittel");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Check recent lebensmittel entries
    $stmt = $pdo->prepare("
        SELECT l.*, 
               sl.name as storage_location_name,
               p.name as package_name
        FROM lebensmittel l
        LEFT JOIN storage_locations sl ON l.storage_location_id = sl.id
        LEFT JOIN packages p ON l.package_id = p.id
        ORDER BY l.id DESC 
        LIMIT 10
    ");
    $stmt->execute();
    $recent_items = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Check storage_locations
    $stmt = $pdo->prepare("SELECT * FROM storage_locations WHERE household_id = 1 LIMIT 5");
    $stmt->execute();
    $storage_locations = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Check packages
    $stmt = $pdo->prepare("SELECT * FROM packages LIMIT 5");
    $stmt->execute();
    $packages = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Check if Multi-Tenant columns exist
    $has_household_id = false;
    $has_storage_location_id = false;
    $has_package_id = false;
    $has_package_count = false;
    
    foreach ($columns as $column) {
        if ($column['Field'] === 'household_id') $has_household_id = true;
        if ($column['Field'] === 'storage_location_id') $has_storage_location_id = true;
        if ($column['Field'] === 'package_id') $has_package_id = true;
        if ($column['Field'] === 'package_count') $has_package_count = true;
    }
    
    echo json_encode([
        'database_check' => 'SUCCESS',
        'timestamp' => date('Y-m-d H:i:s'),
        'lebensmittel_columns' => array_column($columns, 'Field'),
        'multi_tenant_columns' => [
            'household_id' => $has_household_id,
            'storage_location_id' => $has_storage_location_id,
            'package_id' => $has_package_id,
            'package_count' => $has_package_count
        ],
        'recent_lebensmittel' => $recent_items,
        'storage_locations_count' => count($storage_locations),
        'storage_locations' => $storage_locations,
        'packages_count' => count($packages),
        'packages' => $packages,
        'items_with_storage_location' => array_filter($recent_items, function($item) {
            return !empty($item['storage_location_id']);
        }),
        'items_with_package' => array_filter($recent_items, function($item) {
            return !empty($item['package_id']);
        })
    ], JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    echo json_encode([
        'ERROR' => $e->getMessage(),
        'trace' => $e->getTraceAsString()
    ], JSON_PRETTY_PRINT);
}
?>
