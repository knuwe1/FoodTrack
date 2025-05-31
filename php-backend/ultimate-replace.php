<?php
// ULTIMATE replacement - standalone version with NO dependencies
$target = __DIR__ . '/endpoints/lebensmittel.php';
$source = __DIR__ . '/standalone-lebensmittel.php';

if (file_exists($source)) {
    $new_content = file_get_contents($source);
    
    // Backup original
    if (file_exists($target)) {
        $backup = file_get_contents($target);
        file_put_contents($target . '.ultimate-backup', $backup);
    }
    
    // Replace with standalone content
    if (file_put_contents($target, $new_content)) {
        echo json_encode([
            'SUCCESS' => 'ULTIMATE REPLACEMENT - endpoints/lebensmittel.php is now STANDALONE!',
            'new_size' => strlen($new_content),
            'file_size' => filesize($target),
            'backup_created' => 'lebensmittel.php.ultimate-backup',
            'features' => [
                'NO external dependencies',
                'Inline auth function',
                'Multi-Tenant support',
                'All CRUD operations',
                'Should work immediately'
            ],
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to replace file']);
    }
} else {
    echo json_encode(['ERROR' => 'Source file not found']);
}
?>
