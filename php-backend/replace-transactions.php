<?php
// Replace endpoints/transactions.php with standalone Multi-Tenant version
$target = __DIR__ . '/endpoints/transactions.php';
$source = __DIR__ . '/standalone-transactions.php';

if (file_exists($source)) {
    $new_content = file_get_contents($source);
    
    // Backup original
    if (file_exists($target)) {
        $backup = file_get_contents($target);
        file_put_contents($target . '.backup', $backup);
    }
    
    // Replace with standalone content
    if (file_put_contents($target, $new_content)) {
        echo json_encode([
            'SUCCESS' => 'endpoints/transactions.php replaced with Multi-Tenant standalone version!',
            'new_size' => strlen($new_content),
            'file_size' => filesize($target),
            'backup_created' => 'transactions.php.backup',
            'features' => [
                'NO external dependencies',
                'Inline auth function',
                'Multi-Tenant household filtering',
                'FIFO batch consumption',
                'Purchase and consumption tracking',
                'Should fix Einkaufen/Verbrauchen functionality'
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
