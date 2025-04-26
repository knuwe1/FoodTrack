# backend/tests/test_lebensmittel.py

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session # Import Session
from src.main import app
from src.db.session import TestingSessionLocal # Import TestingSessionLocal aus conftest
from src.crud import lebensmittel as crud_lebensmittel # Importiere CRUD für Setup/Teardown
from src.schemas.lebensmittel import LebensmittelCreate

# Client Fixture bleibt auf Modulebene, wenn die App-Instanz für alle Tests gleich ist
@pytest.fixture(scope="module")
def client():
    yield TestClient(app) # Verwende yield für korrekten Teardown

# Beispiel für eine Session-Fixture auf Funktionsebene (aus conftest inspiriert)
@pytest.fixture(scope="function")
def db_session():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        # Optional: Hier könnten nach jedem Test Daten bereinigt werden,
        # aber da wir :memory: verwenden, ist das oft nicht nötig.
        db.close()

# Fixture für ein erstelltes Item auf Funktionsebene für Isolation
@pytest.fixture(scope="function")
def created_item(client: TestClient, db_session: Session):
    payload = {"name": "TestApfel", "quantity": 5, "einheit": "Stück"}
    # Erstelle Item direkt über CRUD für Kontrolle oder über API
    item_schema = LebensmittelCreate(**payload)
    db_item = crud_lebensmittel.create_lebensmittel(db=db_session, lebensmittel_in=item_schema)
    # Gebe die ID oder das Objekt zurück, je nachdem was die Tests brauchen
    # Wichtig: Da wir eine separate Session pro Test nutzen, ist das Item
    # nach dem Test nicht mehr in der DB, es sei denn, die DB ist :memory:
    # und die Engine bleibt bestehen.
    # Für API-Tests ist es oft besser, über die API zu gehen:
    # response = client.post("/api/v1/lebensmittel/", json=payload)
    # assert response.status_code == 201 # Angepasst an neuen Statuscode
    # yield response.json() # Nutze yield, wenn Teardown (z.B. Löschen) nötig ist
    # Hier vereinfacht, Annahme dass CRUD direkt ok ist für Test-Setup:
    yield {"id": db_item.id, "name": db_item.name, "quantity": db_item.menge, "einheit": db_item.einheit}
    # Nach dem Test wird das Item implizit durch die :memory: DB oder explizites Löschen entfernt


# --- Tests verwenden jetzt die 'function' scoped fixtures ---

def test_create_lebensmittel(client: TestClient):
    # Testet das Erstellen isoliert
    payload = {"name": "Birne", "quantity": 10, "einheit": "kg"}
    response = client.post("/api/v1/lebensmittel/", json=payload)
    # Prüfe auf 201 CREATED
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Birne"
    assert data["quantity"] == 10 # Prüfe auf Alias
    assert data["einheit"] == "kg"
    assert "id" in data

# Test für das Lesen der Liste
def test_read_lebensmittel_list(client: TestClient, created_item): # Fügt ein Item hinzu für den Test
    response = client.get("/api/v1/lebensmittel/")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # Prüfe, ob das erstellte Item in der Liste ist
    assert any(item['id'] == created_item['id'] for item in data)
    assert any(item['name'] == created_item['name'] for item in data)


def test_read_lebensmittel_by_id(client: TestClient, created_item):
    item_id = created_item["id"]
    response = client.get(f"/api/v1/lebensmittel/{item_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == item_id
    assert data["name"] == created_item["name"]
    assert data["quantity"] == created_item["quantity"] # Prüfe auf Alias


def test_update_lebensmittel(client: TestClient, created_item):
    item_id = created_item["id"]
    # Wichtig: Pydantic v2 model_dump erzeugt standardmäßig keine Aliase,
    # aber FastAPI's Request-Body-Deserialisierung verwendet sie.
    # Hier senden wir den Alias 'quantity'.
    update_payload = {"quantity": 20, "kategorie": "Obst"}
    response = client.put(f"/api/v1/lebensmittel/{item_id}", json=update_payload)
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == item_id
    assert data["quantity"] == 20 # Prüfe auf Alias
    assert data["kategorie"] == "Obst"
    # Optional: Prüfen, dass andere Felder unverändert sind
    assert data["name"] == created_item["name"]


def test_delete_lebensmittel(client: TestClient, created_item):
    item_id = created_item["id"]
    response = client.delete(f"/api/v1/lebensmittel/{item_id}")
    assert response.status_code == 204 # Prüfe auf NO CONTENT

    # Prüfe, ob das Item wirklich gelöscht wurde
    response_get = client.get(f"/api/v1/lebensmittel/{item_id}")
    assert response_get.status_code == 404 # Prüfe auf NOT FOUND