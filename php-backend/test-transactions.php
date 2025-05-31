<?php
// Test if endpoints/transactions.php has syntax errors
echo "Testing endpoints/transactions.php...\n";

// Check if file exists
$file = __DIR__ . '/endpoints/transactions.php';
if (!file_exists($file)) {
    echo json_encode(['ERROR' => 'endpoints/transactions.php does not exist']);
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
            'get_transactions' => function_exists('get_transactions'),
            'create_transaction' => function_exists('create_transaction'),
            'record_purchase' => function_exists('record_purchase'),
            'record_consumption' => function_exists('record_consumption'),
            'requireAuth' => function_exists('requireAuth')
        ]
    ]);
} catch (ParseError $e) {
    echo json_encode([
        'ERROR' => 'Syntax error in endpoints/transactions.php',
        'message' => $e->getMessage(),
        'line' => $e->getLine()
    ]);
} catch (Error $e) {
    echo json_encode([
        'ERROR' => 'Fatal error in endpoints/transactions.php',
        'message' => $e->getMessage(),
        'line' => $e->getLine()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'ERROR' => 'Exception in endpoints/transactions.php',
        'message' => $e->getMessage()
    ]);
}
?>
