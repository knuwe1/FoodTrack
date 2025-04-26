#!/usr/bin/env bash

# Voraussetzungen prüfen
if ! command -v python3 &> /dev/null; then
    echo "python3 nicht gefunden. Bitte installiere python3 und python3-venv." >&2
    exit 1
fi

# Projekt-Root ermitteln
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."
cd "${PROJECT_ROOT}"

# Virtual Environment erstellen
python3 -m venv backend/venv

# venv aktivieren
# shellcheck source=/dev/null
source backend/venv/bin/activate

# Abhängigkeiten installieren
pip install --upgrade pip
pip install -r backend/requirements.txt

echo "Setup abgeschlossen! Virtualenv in backend/venv erstellt und Dependencies installiert."