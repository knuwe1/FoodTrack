<?php
header('Content-Type: text/plain');

echo "Checking current routes in index.php...\n";
echo "=====================================\n\n";

$index_file = __DIR__ . '/index.php';

if (file_exists($index_file)) {
    $content = file_get_contents($index_file);
    
    echo "File exists: YES\n";
    echo "File size: " . strlen($content) . " bytes\n";
    echo "Last modified: " . date('Y-m-d H:i:s', filemtime($index_file)) . "\n\n";
    
    // Suche nach spezifischen Routen
    $routes_to_check = [
        '/debug/' => 'Debug endpoint',
        '/test-households/' => 'Test households endpoint',
        '/households/' => 'Households endpoint',
        'requireAuth()' => 'Auth middleware call'
    ];
    
    echo "Route analysis:\n";
    echo "---------------\n";
    
    foreach ($routes_to_check as $pattern => $description) {
        $found = strpos($content, $pattern) !== false;
        echo sprintf("%-25s: %s\n", $description, $found ? 'FOUND' : 'NOT FOUND');
    }
    
    echo "\nLast 20 lines of index.php:\n";
    echo "----------------------------\n";
    $lines = explode("\n", $content);
    $last_lines = array_slice($lines, -20);
    foreach ($last_lines as $i => $line) {
        echo sprintf("%3d: %s\n", count($lines) - 20 + $i + 1, $line);
    }
    
} else {
    echo "File does NOT exist!\n";
}
?>
