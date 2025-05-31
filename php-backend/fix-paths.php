<?php
// Fix paths in endpoints/lebensmittel.php
$target = __DIR__ . '/endpoints/lebensmittel.php';

if (file_exists($target)) {
    $content = file_get_contents($target);
    
    // Fix the require_once paths
    $content = str_replace(
        "require_once __DIR__ . '/../config/database.php';",
        "// Path fixed - config loaded in index.php",
        $content
    );
    
    $content = str_replace(
        "require_once __DIR__ . '/../middleware/auth.php';",
        "require_once __DIR__ . '/middleware/auth.php';",
        $content
    );
    
    $content = str_replace(
        "require_once __DIR__ . '/../utils/response.php';",
        "require_once __DIR__ . '/utils/response.php';",
        $content
    );
    
    // Backup original
    file_put_contents($target . '.backup', file_get_contents($target));
    
    if (file_put_contents($target, $content)) {
        echo json_encode([
            'SUCCESS' => 'Paths fixed in endpoints/lebensmittel.php!',
            'changes' => [
                'config/database.php' => 'Removed (already loaded in index.php)',
                'middleware/auth.php' => 'Fixed path',
                'utils/response.php' => 'Fixed path'
            ],
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to write file']);
    }
} else {
    echo json_encode(['ERROR' => 'endpoints/lebensmittel.php not found']);
}
?>
