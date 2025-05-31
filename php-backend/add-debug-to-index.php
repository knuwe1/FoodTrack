<?php
// Add debug info to index.php
$target = __DIR__ . '/index.php';

if (file_exists($target)) {
    $content = file_get_contents($target);
    
    // Add debug code at the beginning (after <?php)
    $debug_code = '
// DEBUG: Log all requests
$debug_info = [
    "debug_routing" => "ACTIVE",
    "timestamp" => date("Y-m-d H:i:s"),
    "request_uri" => $_SERVER["REQUEST_URI"] ?? "not set",
    "request_method" => $_SERVER["REQUEST_METHOD"] ?? "not set",
    "script_name" => $_SERVER["SCRIPT_NAME"] ?? "not set",
    "current_file" => __FILE__
];
file_put_contents(__DIR__ . "/debug.log", json_encode($debug_info) . "\n", FILE_APPEND);
';

    // Insert after <?php
    $new_content = str_replace('<?php', '<?php' . $debug_code, $content);
    
    if ($new_content !== $content) {
        // Backup original
        file_put_contents($target . '.backup', $content);
        
        if (file_put_contents($target, $new_content)) {
            echo json_encode([
                'SUCCESS' => 'Debug code added to index.php!',
                'backup_created' => 'index.php.backup',
                'timestamp' => date('Y-m-d H:i:s')
            ]);
        } else {
            echo json_encode(['ERROR' => 'Failed to write index.php']);
        }
    } else {
        echo json_encode(['ERROR' => 'Could not modify index.php']);
    }
} else {
    echo json_encode(['ERROR' => 'index.php not found']);
}
?>
