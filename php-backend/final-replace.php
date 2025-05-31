<?php
// Final replacement of endpoints/lebensmittel.php
$target = __DIR__ . '/endpoints/lebensmittel.php';
$source = __DIR__ . '/working-lebensmittel.php';

if (file_exists($source)) {
    $new_content = file_get_contents($source);
    
    // Backup original
    if (file_exists($target)) {
        $backup = file_get_contents($target);
        file_put_contents($target . '.final-backup', $backup);
    }
    
    // Replace with working content
    if (file_put_contents($target, $new_content)) {
        echo json_encode([
            'SUCCESS' => 'endpoints/lebensmittel.php FINAL REPLACEMENT completed!',
            'new_size' => strlen($new_content),
            'file_size' => filesize($target),
            'backup_created' => 'lebensmittel.php.final-backup',
            'timestamp' => date('Y-m-d H:i:s'),
            'message' => 'This should fix all path issues and add Multi-Tenant support!'
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to replace file']);
    }
} else {
    echo json_encode(['ERROR' => 'Source file not found']);
}
?>
