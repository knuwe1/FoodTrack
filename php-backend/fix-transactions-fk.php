<?php
// Fix Foreign Key constraint in endpoints/transactions.php
$target = __DIR__ . '/endpoints/transactions.php';

if (file_exists($target)) {
    $content = file_get_contents($target);
    
    // Fix all INSERT statements to include created_by
    
    // Fix 1: create_transaction function - purchase INSERT
    $content = str_replace(
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
                VALUES (?, ?, ?, ?, ?)',
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd, created_by)
                VALUES (?, ?, ?, ?, ?, ?)',
        $content
    );
    
    // Fix 2: create_transaction function - consumption INSERT
    $content = str_replace(
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
                    VALUES (?, ?, ?, ?, ?)',
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd, created_by)
                    VALUES (?, ?, ?, ?, ?, ?)',
        $content
    );
    
    // Fix 3: record_purchase function
    $content = str_replace(
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
            VALUES (?, ?, \'purchase\', ?, ?)',
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd, created_by)
            VALUES (?, ?, \'purchase\', ?, ?, ?)',
        $content
    );
    
    // Fix 4: record_consumption function
    $content = str_replace(
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd)
                VALUES (?, ?, \'consumption\', ?, ?)',
        'INSERT INTO transactions (lebensmittel_id, batch_id, transaction_type, menge, mhd, created_by)
                VALUES (?, ?, \'consumption\', ?, ?, ?)',
        $content
    );
    
    // Now fix the execute statements to include $user['id']
    
    // Fix execute for create_transaction - purchase
    $content = str_replace(
        '$stmt->execute([$lebensmittel_id, $batch_id, $transaction_type, $menge, $mhd]);',
        '$stmt->execute([$lebensmittel_id, $batch_id, $transaction_type, $menge, $mhd, $user[\'id\']]);',
        $content
    );
    
    // Fix execute for create_transaction - consumption
    $content = str_replace(
        '$stmt->execute([$lebensmittel_id, $batch_id, $transaction_type, $consume_from_batch, $batch[\'ablaufdatum\']]);',
        '$stmt->execute([$lebensmittel_id, $batch_id, $transaction_type, $consume_from_batch, $batch[\'ablaufdatum\'], $user[\'id\']]);',
        $content
    );
    
    // Fix execute for record_purchase
    $content = str_replace(
        '$stmt->execute([$lebensmittel_id, $batch_id, $quantity, $mhd]);',
        '$stmt->execute([$lebensmittel_id, $batch_id, $quantity, $mhd, $user[\'id\']]);',
        $content
    );
    
    // Fix execute for record_consumption
    $content = str_replace(
        '$stmt->execute([$lebensmittel_id, $batch_id, $consume_from_batch, $batch[\'ablaufdatum\']]);',
        '$stmt->execute([$lebensmittel_id, $batch_id, $consume_from_batch, $batch[\'ablaufdatum\'], $user[\'id\']]);',
        $content
    );
    
    // Backup original
    file_put_contents($target . '.fk-backup', file_get_contents($target));
    
    if (file_put_contents($target, $content)) {
        echo json_encode([
            'SUCCESS' => 'Foreign Key constraints fixed in endpoints/transactions.php!',
            'changes' => [
                'Added created_by column to all INSERT statements',
                'Added $user[\'id\'] parameter to all execute calls',
                'Should fix the Foreign Key constraint violation'
            ],
            'backup_created' => 'transactions.php.fk-backup',
            'timestamp' => date('Y-m-d H:i:s')
        ]);
    } else {
        echo json_encode(['ERROR' => 'Failed to write file']);
    }
} else {
    echo json_encode(['ERROR' => 'endpoints/transactions.php not found']);
}
?>
