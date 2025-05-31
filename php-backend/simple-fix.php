<?php
// Simple fix for lebensmittel.php
$content = file_get_contents(__DIR__ . '/api/v1/lebensmittel.php');

// Replace the getLebensmittelList function with Multi-Tenant version
$old_function = '/function getLebensmittelList\(\$user\) \{.*?sendSuccess\(\$result\);\s*\}/s';

$new_function = 'function getLebensmittelList($user) {
    global $pdo;
    
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
    ");
    $stmt->execute([$user[\'household_id\']]);
    $lebensmittel = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $result = array_map(function($item) {
        return [
            \'id\' => (int)$item[\'id\'],
            \'name\' => $item[\'name\'],
            \'quantity\' => (int)$item[\'quantity\'],
            \'einheit\' => $item[\'einheit\'],
            \'kategorie\' => $item[\'kategorie\'],
            \'ablaufdatum\' => $item[\'ablaufdatum\'],
            \'ean_code\' => $item[\'ean_code\'],
            \'mindestmenge\' => (int)$item[\'mindestmenge\'],
            \'household_id\' => (int)$item[\'household_id\'],
            \'storage_location_id\' => $item[\'storage_location_id\'] ? (int)$item[\'storage_location_id\'] : null,
            \'storage_location_name\' => $item[\'storage_location_name\'],
            \'location_type\' => $item[\'location_type\'],
            \'package_id\' => $item[\'package_id\'] ? (int)$item[\'package_id\'] : null,
            \'package_name\' => $item[\'package_name\'],
            \'package_count\' => (int)$item[\'package_count\'],
            \'package_fill_amount\' => $item[\'fill_amount\'] ? (float)$item[\'fill_amount\'] : null,
            \'package_fill_unit\' => $item[\'fill_unit\'],
            \'package_type\' => $item[\'package_type\'],
            \'created_by\' => (int)$item[\'created_by\'],
            \'updated_by\' => $item[\'updated_by\'] ? (int)$item[\'updated_by\'] : null,
            \'SIMPLE_FIX_SUCCESS\' => \'MULTI-TENANT-WORKING\',
            \'TIMESTAMP\' => date(\'Y-m-d H:i:s\')
        ];
    }, $lebensmittel);

    sendSuccess($result);
}';

$new_content = preg_replace($old_function, $new_function, $content);

if (file_put_contents(__DIR__ . '/api/v1/lebensmittel.php', $new_content)) {
    echo json_encode([
        'SUCCESS' => 'Multi-Tenant function injected!',
        'old_size' => strlen($content),
        'new_size' => strlen($new_content),
        'file_size' => filesize(__DIR__ . '/api/v1/lebensmittel.php'),
        'timestamp' => date('Y-m-d H:i:s')
    ]);
} else {
    echo json_encode([
        'ERROR' => 'Failed to update file',
        'timestamp' => date('Y-m-d H:i:s')
    ]);
}
?>
