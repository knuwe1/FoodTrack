<?php
// Fix .htaccess to use direct files instead of index.php routing
$target = __DIR__ . '/.htaccess';

$new_htaccess_content = 'RewriteEngine On

# IMPORTANT: Pass Authorization header to PHP (multiple methods)
RewriteCond %{HTTP:Authorization} ^(.*)
RewriteRule .* - [e=HTTP_AUTHORIZATION:%1]

# Alternative method for Authorization header
SetEnvIf Authorization "(.*)" HTTP_AUTHORIZATION=$1

# CGI mode compatibility
RewriteCond %{HTTP:Authorization} ^(.+)$
RewriteRule .* - [E=HTTP_AUTHORIZATION:%{HTTP:Authorization}]

# SECURITY: Protect sensitive files and directories
<Files "*.env">
    Order allow,deny
    Deny from all
</Files>

<Files "config.php">
    Order allow,deny
    Deny from all
</Files>

<Files "*.log">
    Order allow,deny
    Deny from all
</Files>

<Files "*.backup">
    Order allow,deny
    Deny from all
</Files>

# Protect all dot files and backup files
<FilesMatch "^\.">
    Order allow,deny
    Deny from all
</FilesMatch>

<FilesMatch "\.(bak|backup|old|tmp)$">
    Order allow,deny
    Deny from all
</FilesMatch>

# Prevent directory browsing
Options -Indexes

# Handle CORS preflight requests
RewriteCond %{REQUEST_METHOD} OPTIONS
RewriteRule ^(.*)$ index.php [QSA,L]

# DIRECT API ROUTES (NEW) - Use corrected files directly
RewriteRule ^api/v1/lebensmittel/?(.*)$ api/v1/lebensmittel.php [QSA,L]
RewriteRule ^api/v1/storage-locations/?(.*)$ api/v1/storage-locations.php [QSA,L]
RewriteRule ^api/v1/packages/?(.*)$ api/v1/packages.php [QSA,L]

# Multi-Tenant API Routes - Route through index.php for other endpoints
RewriteRule ^households/?(.*)$ index.php [QSA,L]

# Route remaining API requests to index.php (fallback)
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^api/v1/(.*)$ index.php [QSA,L]

# Alternative: Route everything else to index.php if no specific file exists
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^(.*)$ index.php [QSA,L]
';

// Backup current .htaccess
$backup = file_get_contents($target);
file_put_contents($target . '.backup', $backup);

// Write new .htaccess
if (file_put_contents($target, $new_htaccess_content)) {
    echo json_encode([
        'SUCCESS' => '.htaccess updated to use direct API files!',
        'changes' => [
            'lebensmittel' => 'Now routes to api/v1/lebensmittel.php directly',
            'storage_locations' => 'Now routes to api/v1/storage-locations.php directly',
            'packages' => 'Now routes to api/v1/packages.php directly',
            'other_endpoints' => 'Still use index.php routing'
        ],
        'backup_created' => '.htaccess.backup',
        'timestamp' => date('Y-m-d H:i:s')
    ]);
} else {
    echo json_encode(['ERROR' => 'Failed to update .htaccess']);
}
?>
