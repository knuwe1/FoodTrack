# FoodTrack

FoodTrack ist eine modulare Anwendung zur Verwaltung von Lebensmittelbeständen im Haushalt.

## Funktionen
- Erfassung und Verwaltung von Lebensmitteln
- Barcode-Scan im Android-Client
- Rezeptverwaltung und Einkaufslisten (in Planung)

## Installation

### Backend (Python + FastAPI)
```bash
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn src.app:app --reload