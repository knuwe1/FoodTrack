<?php
// Test if endpoints/lebensmittel.php has syntax errors
echo "Testing endpoints/lebensmittel.php...\n";

// Check if file exists
$file = __DIR__ . '/endpoints/lebensmittel.php';
if (!file_exists($file)) {
    echo json_encode(['ERROR' => 'endpoints/lebensmittel.php does not exist']);
    exit;
}

// Check file size
$size = filesize($file);
echo "File size: $size bytes\n";

// Try to include the file
try {
    ob_start();
    include_once $file;
    $output = ob_get_clean();
    
    echo json_encode([
        'SUCCESS' => 'File loaded without syntax errors',
        'file_size' => $size,
        'output' => $output,
        'functions_exist' => [
            'get_lebensmittel_list' => function_exists('get_lebensmittel_list'),
            'create_lebensmittel' => function_exists('create_lebensmittel'),
            'get_lebensmittel' => function_exists('get_lebensmittel')
        ]
    ]);
} catch (ParseError $e) {
    echo json_encode([
        'ERROR' => 'Syntax error in endpoints/lebensmittel.php',
        'message' => $e->getMessage(),
        'line' => $e->getLine()
    ]);
} catch (Error $e) {
    echo json_encode([
        'ERROR' => 'Fatal error in endpoints/lebensmittel.php',
        'message' => $e->getMessage(),
        'line' => $e->getLine()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'ERROR' => 'Exception in endpoints/lebensmittel.php',
        'message' => $e->getMessage()
    ]);
}
?>
