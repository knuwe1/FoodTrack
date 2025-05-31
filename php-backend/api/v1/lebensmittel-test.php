<?php
// TEST VERSION - Multi-Tenant lebensmittel.php
require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../middleware/auth.php';
require_once __DIR__ . '/../../utils/response.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Require authentication for all lebensmittel operations
$user = requireAuth();

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        // Simple test response with Multi-Tenant fields
        sendSuccess([
            [
                'id' => 999,
                'name' => 'TEST MULTI-TENANT WORKING',
                'quantity' => 1,
                'einheit' => 'Stück',
                'kategorie' => 'Test',
                'ablaufdatum' => null,
                'ean_code' => null,
                'mindestmenge' => 0,
                'household_id' => $user['household_id'],
                'storage_location_id' => 1,
                'storage_location_name' => 'Kühlschrank',
                'location_type' => 'refrigerator',
                'package_id' => 1,
                'package_name' => '1 Stück',
                'package_count' => 1,
                'package_fill_amount' => 1.0,
                'package_fill_unit' => 'Stück',
                'package_type' => 'piece',
                'created_by' => $user['id'],
                'updated_by' => null,
                'DEBUG_VERSION' => 'TEST-MULTI-TENANT-v3.0',
                'DEBUG_TIME' => date('Y-m-d H:i:s')
            ]
        ]);
    } else {
        sendError('Method not allowed', 405);
    }
} catch (Exception $e) {
    sendError('Internal server error: ' . $e->getMessage(), 500);
}
?>
