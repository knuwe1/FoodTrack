<?php
// Fix endpoints/lebensmittel.php with Multi-Tenant fields
$target = __DIR__ . '/endpoints/lebensmittel.php';

// Read current content
$content = file_get_contents($target);

// Replace the get_lebensmittel_list function with Multi-Tenant version
$old_function = '/function get_lebensmittel_list\(\$pdo\) \{.*?echo json_encode\(\$result\);\s*\}/s';

$new_function = 'function get_lebensmittel_list($pdo) {
    try {
        // Get user info for Multi-Tenant filtering
        require_once __DIR__ . \'/../middleware/auth.php\';
        $user = requireAuth();

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

        // Convert to expected format with Multi-Tenant fields
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
                \'ENDPOINTS_FIXED\' => \'MULTI_TENANT_SUCCESS\',
                \'TIMESTAMP\' => date(\'Y-m-d H:i:s\')
            ];
        }, $lebensmittel);

        echo json_encode($result);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([\'error\' => \'Database error: \' . $e->getMessage()]);
    }
}';

$new_content = preg_replace($old_function, $new_function, $content);

if ($new_content && $new_content !== $content) {
    // Backup original
    file_put_contents($target . '.backup', $content);
    
    if (file_put_contents($target, $new_content)) {
        echo json_encode([
            'SUCCESS' => 'endpoints/lebensmittel.php fixed with Multi-Tenant!',
            'old_size' => strlen($content),
            'new_size' => strlen($new_content),
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to write file']);
    }
} else {
    echo json_encode(['ERROR' => 'Pattern not found or no changes made']);
}
?>
