# FoodTrack

FoodTrack ist eine modulare Anwendung zur Verwaltung von Lebensmittelbeständen im Haushalt.

## Funktionen
- Erfassung und Verwaltung von Lebensmitteln ([cite: 1])
- Benutzerregistrierung und Authentifizierung ()
- Barcode-Scan im Android-Client (falls vorhanden, nicht im Backend-Code ersichtlich) ([cite: 1])
- Rezeptverwaltung und Einkaufslisten (in Planung) ([cite: 1])

## Installation

### Voraussetzungen
- Python 3.x
- `python3-venv`

### Backend (Python + FastAPI)

1.  **Repository klonen:**
    ```bash
    git clone <repository-url>
    cd FoodTrack-main
    ```

2.  **Setup-Skript ausführen (empfohlen):**
    Das Skript erstellt ein Virtual Environment und installiert die Abhängigkeiten.
    ```bash
    bash scripts/setup.sh
    ```
    Oder manuell:
    ```bash
    cd backend
    python3 -m venv venv
    source venv/bin/activate  # Auf Windows: venv\Scripts\activate
    pip install --upgrade pip
    pip install -r requirements.txt
    ```

3.  **Umgebungsvariablen konfigurieren:**
    Erstelle eine Datei `.env` im Verzeichnis `FoodTrack-main/backend/` mit folgendem Inhalt (passe die Werte an):
    ```dotenv
    # Beispiel .env Datei
    # Siehe backend/src/config.py für alle benötigten Variablen

    # Erforderlich für Datenbankverbindung (Beispiel für PostgreSQL)
    DATABASE_URL="postgresql://user:password@host:port/database_name"
    # Beispiel für SQLite (einfacher für lokale Entwicklung):
    # DATABASE_URL="sqlite:///./foodtrack.db"

    # Erforderlich für JWT Token
    SECRET_KEY="dein_sehr_geheimer_schluessel_hier_aendern" # Generiere einen sicheren Schlüssel!

    # Optional (hat Standardwert in config.py)
    ACCESS_TOKEN_EXPIRE_MINUTES=30
    ```
    *Hinweis:* Füge die `.env`-Datei zu deiner `.gitignore`-Datei hinzu, um sensible Daten nicht zu versionieren. Eine Beispieldatei `backend/.env.example` listet die benötigten Umgebungsvariablen auf. Kopiere diese Datei zu `backend/.env` und trage deine eigenen Werte ein.

4.  **Datenbank initialisieren:**
    Das Backend versucht beim Start, die Tabellen zu erstellen (siehe `backend/src/main.py` und `backend/src/db/session.py`). Stelle sicher, dass die Datenbank (z.B. PostgreSQL Server) läuft und zugänglich ist, bevor du die Anwendung startest.

5.  **Anwendung starten:**
    Stelle sicher, dass das Virtual Environment aktiviert ist (`source backend/venv/bin/activate`).
    ```bash
    cd backend
    uvicorn src.main:app --reload --host 0.0.0.0 --port 8000
    ```
    Die API ist dann unter `http://localhost:8000` verfügbar, die interaktive Dokumentation (Swagger UI) unter `http://localhost:8000/docs`.

## API Endpunkte
Die API-Endpunkte sind über FastAPI's Swagger UI unter `/docs` oder ReDoc unter `/redoc` dokumentiert, wenn die Anwendung läuft. Haupt-Router befinden sich in `backend/src/api/v1/router.py`.

- `/api/v1/lebensmittel/`: CRUD-Operationen für Lebensmittel
- `/api/v1/users/`: Benutzerregistrierung, Login und Abruf von Benutzerdetails
- `/health`: Einfacher Health Check

## Tests
Die Tests verwenden `pytest` und eine In-Memory-SQLite-Datenbank.
```bash
cd backend
source venv/bin/activate
pytest