<?php
// Restore original .htaccess
$target = __DIR__ . '/.htaccess';
$backup = __DIR__ . '/.htaccess.backup';

if (file_exists($backup)) {
    $original_content = file_get_contents($backup);
    
    if (file_put_contents($target, $original_content)) {
        echo json_encode([
            'SUCCESS' => '.htaccess restored from backup!',
            'timestamp' => date('Y-m-d H:i:s'),
            'message' => 'Original routing restored'
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to restore .htaccess']);
    }
} else {
    echo json_encode(['ERROR' => 'Backup file not found']);
}
?>
