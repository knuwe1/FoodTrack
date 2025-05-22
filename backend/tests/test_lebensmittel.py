# backend/tests/test_lebensmittel.py

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session # Import Session (wird als Typ-Hinweis benötigt)

# Entferne lokale Fixture-Definition und unnötige Imports
# from .conftest import TestingSessionLocal # Nicht benötigt
# @pytest.fixture(scope="function") ... # Nicht benötigt

from src.main import app # Wird für TestClient benötigt
from src.crud import lebensmittel as crud_lebensmittel
from src.schemas.lebensmittel import LebensmittelCreate

# Die 'client' und 'db_session' Fixtures kommen jetzt aus conftest.py

# Fixture für ein erstelltes Item auf Funktionsebene für Isolation
# Verwendet jetzt die db_session Fixture aus conftest.py
@pytest.fixture(scope="function")
def created_item(client: TestClient, db_session: Session):
    payload = {"name": "TestApfel", "quantity": 5, "einheit": "Stück"}
    item_schema = LebensmittelCreate(**payload)
    # Verwende die db_session Fixture für CRUD Operationen
    db_item = crud_lebensmittel.create_lebensmittel(db=db_session, lebensmittel_in=item_schema)
    # db_session wird automatisch committen/rollbacken/schließen
    yield {"id": db_item.id, "name": db_item.name, "quantity": db_item.menge, "einheit": db_item.einheit}


# --- Tests bleiben gleich, verwenden aber implizit die Fixtures aus conftest ---

def test_create_lebensmittel(client: TestClient):
    payload = {"name": "Birne", "quantity": 10, "einheit": "kg"}
    response = client.post("/api/v1/lebensmittel/", json=payload)
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Birne"
    assert data["quantity"] == 10
    assert data["einheit"] == "kg"
    assert "id" in data

def test_read_lebensmittel_list(client: TestClient, created_item):
    response = client.get("/api/v1/lebensmittel/")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert any(item['id'] == created_item['id'] for item in data)
    assert any(item['name'] == created_item['name'] for item in data)

def test_read_lebensmittel_by_id(client: TestClient, created_item):
    item_id = created_item["id"]
    response = client.get(f"/api/v1/lebensmittel/{item_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == item_id
    assert data["name"] == created_item["name"]
    assert data["quantity"] == created_item["quantity"]

def test_update_lebensmittel(client: TestClient, created_item):
    item_id = created_item["id"]
    update_payload = {"quantity": 20, "kategorie": "Obst"}
    response = client.patch(f"/api/v1/lebensmittel/{item_id}", json=update_payload) # Changed to PATCH
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == item_id
    assert data["quantity"] == 20
    assert data["kategorie"] == "Obst"
    assert data["name"] == created_item["name"]

def test_delete_lebensmittel(client: TestClient, created_item):
    item_id = created_item["id"]
    response = client.delete(f"/api/v1/lebensmittel/{item_id}")
    assert response.status_code == 204

    response_get = client.get(f"/api/v1/lebensmittel/{item_id}")
    assert response_get.status_code == 404


# --- Tests for 404 errors on non-existent items ---

def test_read_non_existent_lebensmittel(client: TestClient):
    response = client.get("/api/v1/lebensmittel/99999")
    assert response.status_code == 404

def test_update_non_existent_lebensmittel(client: TestClient):
    update_payload = {"quantity": 10, "kategorie": "Gemüse"}
    response = client.patch("/api/v1/lebensmittel/99999", json=update_payload) # Changed to PATCH
    assert response.status_code == 404

def test_delete_non_existent_lebensmittel(client: TestClient):
    response = client.delete("/api/v1/lebensmittel/99999")
    assert response.status_code == 404


# --- Tests for input validation errors (quantity > 0) ---

def test_create_lebensmittel_invalid_quantity(client: TestClient):
    payload_zero = {"name": "TestInvalidZero", "quantity": 0, "einheit": "Stk"}
    response_zero = client.post("/api/v1/lebensmittel/", json=payload_zero)
    assert response_zero.status_code == 422

    payload_negative = {"name": "TestInvalidNegative", "quantity": -1, "einheit": "Stk"}
    response_negative = client.post("/api/v1/lebensmittel/", json=payload_negative)
    assert response_negative.status_code == 422

def test_update_lebensmittel_invalid_quantity(client: TestClient, created_item):
    item_id = created_item["id"]
    
    update_payload_zero = {"quantity": 0}
    response_zero = client.patch(f"/api/v1/lebensmittel/{item_id}", json=update_payload_zero) # Changed to PATCH
    assert response_zero.status_code == 422

    update_payload_negative = {"quantity": -1}
    response_negative = client.patch(f"/api/v1/lebensmittel/{item_id}", json=update_payload_negative) # Changed to PATCH
    assert response_negative.status_code == 422