<?php
header('Content-Type: text/plain');

echo "Testing .htaccess and Authorization headers\n";
echo "==========================================\n\n";

// Prüfe .htaccess
$htaccess_file = __DIR__ . '/.htaccess';
if (file_exists($htaccess_file)) {
    echo ".htaccess exists: YES\n";
    echo "Size: " . filesize($htaccess_file) . " bytes\n";
    echo "Last modified: " . date('Y-m-d H:i:s', filemtime($htaccess_file)) . "\n\n";
    
    $content = file_get_contents($htaccess_file);
    echo "Content preview (first 500 chars):\n";
    echo substr($content, 0, 500) . "\n\n";
    
    // Suche nach Authorization-relevanten Regeln
    if (strpos($content, 'Authorization') !== false) {
        echo "Authorization mentioned in .htaccess: YES\n";
    } else {
        echo "Authorization mentioned in .htaccess: NO\n";
    }
    
} else {
    echo ".htaccess exists: NO\n";
}

echo "\nServer environment:\n";
echo "PHP Version: " . phpversion() . "\n";
echo "Server Software: " . ($_SERVER['SERVER_SOFTWARE'] ?? 'unknown') . "\n";
echo "Request Method: " . $_SERVER['REQUEST_METHOD'] . "\n";
echo "Request URI: " . $_SERVER['REQUEST_URI'] . "\n";

echo "\nAll headers:\n";
foreach (getallheaders() as $name => $value) {
    echo "$name: $value\n";
}

echo "\nAuthorization-related server vars:\n";
foreach ($_SERVER as $key => $value) {
    if (stripos($key, 'auth') !== false || stripos($key, 'bearer') !== false) {
        echo "$key: $value\n";
    }
}
?>
