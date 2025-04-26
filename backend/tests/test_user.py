import pytest
from datetime import datetime
from fastapi.testclient import TestClient
from src.main import app

def test_register_and_login(client: TestClient):
    # Registrierung
    register_payload = {"email": "test@example.com", "password": "secret"}
    response = client.post("/api/v1/users/register", json=register_payload)
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "test@example.com"
    assert "id" in data

    # Login
    login_payload = {"username": "test@example.com", "password": "secret"}
    response = client.post("/api/v1/users/login", data=login_payload)
    assert response.status_code == 200
    token_data = response.json()
    assert "access_token" in token_data
    assert token_data["token_type"] == "bearer"

    # Zugriff auf geschützten Endpunkt
    headers = {"Authorization": f"Bearer {token_data['access_token']}"}
    response = client.get("/api/v1/users/me", headers=headers)
    assert response.status_code == 200
    me = response.json()
    assert me["email"] == "test@example.com"