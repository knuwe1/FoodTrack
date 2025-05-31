<?php
// Replace endpoints/lebensmittel.php completely
$target = __DIR__ . '/endpoints/lebensmittel.php';
$source = __DIR__ . '/new-endpoints-lebensmittel.php';

if (file_exists($source)) {
    $new_content = file_get_contents($source);
    
    // Backup original
    if (file_exists($target)) {
        $backup = file_get_contents($target);
        file_put_contents($target . '.backup', $backup);
    }
    
    // Replace with new content
    if (file_put_contents($target, $new_content)) {
        echo json_encode([
            'SUCCESS' => 'endpoints/lebensmittel.php completely replaced!',
            'new_size' => strlen($new_content),
            'file_size' => filesize($target),
            'backup_created' => 'lebensmittel.php.backup',
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to replace file']);
    }
} else {
    echo json_encode(['ERROR' => 'Source file not found']);
}
?>
