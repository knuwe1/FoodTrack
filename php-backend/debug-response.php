<?php
// Debug response test
$target = __DIR__ . '/api/v1/lebensmittel.php';

// Add a unique test response at the beginning
$content = file_get_contents($target);

// Find the sendSuccess($result) line and add a test marker
$pattern = '/sendSuccess\(\$result\);/';
$replacement = '// UNIQUE TEST MARKER - ' . date('Y-m-d H:i:s') . '
    if (count($result) > 0) {
        $result[0][\'UNIQUE_TEST_MARKER\'] = \'WORKING_' . date('His') . '\';
    }
    sendSuccess($result);';

$new_content = preg_replace($pattern, $replacement, $content, 1);

if ($new_content && $new_content !== $content) {
    if (file_put_contents($target, $new_content)) {
        echo json_encode([
            'SUCCESS' => 'Debug marker added!',
            'marker' => 'WORKING_' . date('His'),
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to write file']);
    }
} else {
    echo json_encode(['ERROR' => 'Pattern not found or no changes made']);
}
?>
