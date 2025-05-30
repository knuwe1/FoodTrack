#!/usr/bin/env python3
"""
Einfaches Backend für FoodTrack App
Läuft ohne komplexe Dependencies
"""

import json
import sqlite3
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import threading

# Globale Datenbank-Verbindung
DB_FILE = "simple_foodtrack.db"

def init_db():
    """Initialisiert die SQLite-Datenbank"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()

    # Users Tabelle
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT UNIQUE NOT NULL,
            password TEXT NOT NULL
        )
    ''')

    # Lebensmittel Tabelle
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS lebensmittel (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            menge INTEGER,
            einheit TEXT,
            kategorie TEXT,
            ablaufdatum TEXT
        )
    ''')

    # Test-User erstellen
    cursor.execute('''
        INSERT OR IGNORE INTO users (email, password)
        VALUES ('test@example.com', 'testpass123')
    ''')

    conn.commit()
    conn.close()

class FoodTrackHandler(BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        """Handle CORS preflight requests"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()

    def log_message(self, format, *args):
        """Override to reduce log spam"""
        pass

    def do_POST(self):
        """Handle POST requests"""
        self.send_header('Access-Control-Allow-Origin', '*')

        path = urlparse(self.path).path

        if path == '/api/v1/users/login':
            self.handle_login()
        elif path == '/api/v1/lebensmittel/':
            self.handle_create_lebensmittel()
        else:
            self.send_error(404, "Not Found")

    def do_GET(self):
        """Handle GET requests"""
        self.send_header('Access-Control-Allow-Origin', '*')

        path = urlparse(self.path).path

        if path == '/api/v1/lebensmittel/':
            self.handle_get_lebensmittel()
        else:
            self.send_error(404, "Not Found")

    def do_PATCH(self):
        """Handle PATCH requests"""
        self.send_header('Access-Control-Allow-Origin', '*')

        path = urlparse(self.path).path

        if path.startswith('/api/v1/lebensmittel/'):
            lebensmittel_id = path.split('/')[-1]
            self.handle_update_lebensmittel(lebensmittel_id)
        else:
            self.send_error(404, "Not Found")

    def do_DELETE(self):
        """Handle DELETE requests"""
        self.send_header('Access-Control-Allow-Origin', '*')

        path = urlparse(self.path).path

        if path.startswith('/api/v1/lebensmittel/'):
            lebensmittel_id = path.split('/')[-1]
            self.handle_delete_lebensmittel(lebensmittel_id)
        else:
            self.send_error(404, "Not Found")

    def handle_login(self):
        """Handle user login"""
        try:
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)

            # Parse form data
            data = post_data.decode('utf-8')
            params = parse_qs(data)

            username = params.get('username', [''])[0]
            password = params.get('password', [''])[0]

            # Simple authentication
            if username == 'test@example.com' and password == 'testpass123':
                response = {
                    "access_token": "fake_token_12345",
                    "token_type": "bearer"
                }
                self.send_json_response(response)
            else:
                self.send_error(401, "Invalid credentials")

        except Exception as e:
            print(f"Login error: {e}")
            self.send_error(500, "Internal Server Error")

    def handle_get_lebensmittel(self):
        """Get all lebensmittel"""
        try:
            conn = sqlite3.connect(DB_FILE)
            cursor = conn.cursor()

            cursor.execute('SELECT id, name, menge, einheit, kategorie, ablaufdatum FROM lebensmittel')
            rows = cursor.fetchall()

            lebensmittel_list = []
            for row in rows:
                lebensmittel_list.append({
                    "id": row[0],
                    "name": row[1],
                    "menge": row[2],
                    "einheit": row[3],
                    "kategorie": row[4],
                    "ablaufdatum": row[5]
                })

            conn.close()
            self.send_json_response(lebensmittel_list)

        except Exception as e:
            print(f"Get lebensmittel error: {e}")
            self.send_error(500, "Internal Server Error")

    def handle_create_lebensmittel(self):
        """Create new lebensmittel"""
        try:
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode('utf-8'))

            conn = sqlite3.connect(DB_FILE)
            cursor = conn.cursor()

            cursor.execute('''
                INSERT INTO lebensmittel (name, menge, einheit, kategorie, ablaufdatum)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                data.get('name'),
                data.get('quantity'),  # Backend erwartet 'quantity'
                data.get('einheit'),
                data.get('kategorie'),
                data.get('ablaufdatum')
            ))

            lebensmittel_id = cursor.lastrowid
            conn.commit()

            # Return created item
            cursor.execute('SELECT id, name, menge, einheit, kategorie, ablaufdatum FROM lebensmittel WHERE id = ?', (lebensmittel_id,))
            row = cursor.fetchone()

            response = {
                "id": row[0],
                "name": row[1],
                "menge": row[2],
                "einheit": row[3],
                "kategorie": row[4],
                "ablaufdatum": row[5]
            }

            conn.close()
            self.send_json_response(response, 201)

        except Exception as e:
            print(f"Create lebensmittel error: {e}")
            self.send_error(500, "Internal Server Error")

    def handle_update_lebensmittel(self, lebensmittel_id):
        """Update lebensmittel"""
        try:
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode('utf-8'))

            conn = sqlite3.connect(DB_FILE)
            cursor = conn.cursor()

            cursor.execute('''
                UPDATE lebensmittel
                SET name = ?, menge = ?, einheit = ?, kategorie = ?, ablaufdatum = ?
                WHERE id = ?
            ''', (
                data.get('name'),
                data.get('quantity'),  # Backend erwartet 'quantity'
                data.get('einheit'),
                data.get('kategorie'),
                data.get('ablaufdatum'),
                lebensmittel_id
            ))

            conn.commit()

            # Return updated item
            cursor.execute('SELECT id, name, menge, einheit, kategorie, ablaufdatum FROM lebensmittel WHERE id = ?', (lebensmittel_id,))
            row = cursor.fetchone()

            if row:
                response = {
                    "id": row[0],
                    "name": row[1],
                    "menge": row[2],
                    "einheit": row[3],
                    "kategorie": row[4],
                    "ablaufdatum": row[5]
                }
                conn.close()
                self.send_json_response(response)
            else:
                conn.close()
                self.send_error(404, "Lebensmittel not found")

        except Exception as e:
            print(f"Update lebensmittel error: {e}")
            self.send_error(500, "Internal Server Error")

    def handle_delete_lebensmittel(self, lebensmittel_id):
        """Delete lebensmittel"""
        try:
            conn = sqlite3.connect(DB_FILE)
            cursor = conn.cursor()

            cursor.execute('DELETE FROM lebensmittel WHERE id = ?', (lebensmittel_id,))

            if cursor.rowcount > 0:
                conn.commit()
                conn.close()
                self.send_response(204)
                self.end_headers()
            else:
                conn.close()
                self.send_error(404, "Lebensmittel not found")

        except Exception as e:
            print(f"Delete lebensmittel error: {e}")
            self.send_error(500, "Internal Server Error")

    def send_json_response(self, data, status_code=200):
        """Send JSON response"""
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode('utf-8'))

def run_server():
    """Start the HTTP server"""
    init_db()

    server_address = ('0.0.0.0', 8000)
    httpd = HTTPServer(server_address, FoodTrackHandler)

    print("🚀 FoodTrack Backend läuft auf http://0.0.0.0:8000")
    print("📱 Android-App kann sich jetzt verbinden!")
    print("🔑 Login: test@example.com / testpass123")

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Server gestoppt")
        httpd.shutdown()

if __name__ == "__main__":
    run_server()
