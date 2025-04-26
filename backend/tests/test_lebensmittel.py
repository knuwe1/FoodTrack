# backend/tests/test_lebensmittel.py

import pytest
from fastapi.testclient import TestClient

from src.main import app

@pytest.fixture(scope="module")
def client():
    return TestClient(app)


@pytest.fixture(scope="module")
def created_item(client):
    # Erst eine Ressource anlegen
    payload = {"name": "Apfel", "quantity": 5}
    resp = client.post("/api/v1/lebensmittel", json=payload)
    assert resp.status_code == 200
    return resp.json()


def test_create_lebensmittel(created_item):
    assert "id" in created_item
    assert created_item["name"] == "Apfel"
    assert created_item["quantity"] == 5


def test_read_lebensmittel_list(client, created_item):
    resp = client.get("/api/v1/lebensmittel")
    assert resp.status_code == 200
    data = resp.json()
    # Liste muss mindestens unser einen Eintrag enthalten
    assert isinstance(data, list)
    ids = [item["id"] for item in data]
    assert created_item["id"] in ids


def test_read_lebensmittel_by_id(client, created_item):
    mid = created_item["id"]
    resp = client.get(f"/api/v1/lebensmittel/{mid}")
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"] == mid
    assert data["name"] == created_item["name"]
    assert data["quantity"] == created_item["quantity"]


def test_update_lebensmittel(client, created_item):
    mid = created_item["id"]
    update_payload = {"quantity": 10}
    resp = client.put(f"/api/v1/lebensmittel/{mid}", json=update_payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["quantity"] == 10


def test_delete_lebensmittel(client, created_item):
    mid = created_item["id"]
    resp = client.delete(f"/api/v1/lebensmittel/{mid}")
    assert resp.status_code == 204
    # Danach sollte 404 kommen
    resp2 = client.get(f"/api/v1/lebensmittel/{mid}")
    assert resp2.status_code == 404
