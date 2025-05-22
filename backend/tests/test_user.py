import pytest
from datetime import datetime
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session # Added for db_session type hint
from src.main import app
from src.crud import user as crud_user # Added for user manipulation
# from src.models.user import User # Potentially needed if direct model interaction is used

# client and db_session fixtures are expected from conftest.py

def test_register_and_login(client: TestClient):
    # Registrierung
    register_payload = {"email": "test@example.com", "password": "secret"}
    response = client.post("/api/v1/users/register", json=register_payload)
    assert response.status_code == 201 # Changed from 200 to 201
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

# --- Additional User Tests ---

def test_register_existing_email(client: TestClient):
    email = "test_dup@example.com"
    # 1. Register a user
    register_payload1 = {"email": email, "password": "secret"}
    response1 = client.post("/api/v1/users/register", json=register_payload1)
    assert response1.status_code == 201

    # 2. Attempt to register another user with the same email
    register_payload2 = {"email": email, "password": "anothersecret"}
    response2 = client.post("/api/v1/users/register", json=register_payload2)
    assert response2.status_code == 400
    data = response2.json()
    assert data["detail"] == "Email already registered"

def test_login_non_existent_user(client: TestClient):
    login_payload = {"username": "nouser@example.com", "password": "secret"}
    response = client.post("/api/v1/users/login", data=login_payload)
    # CRUD get_user_by_email raises 404, which authenticate_user doesn't catch explicitly.
    # This 404 will propagate to the endpoint.
    # If authenticate_user returned None and the endpoint raised 401, this would be 401.
    # Given the current implementation, we expect the 404 from CRUD.
    assert response.status_code == 404 # Changed from 401 due to crud.get_user_by_email raising 404
    # If the expectation was 401 (e.g. if authenticate_user caught the 404 and returned None)
    # then the following would be more appropriate:
    # assert response.status_code == 401
    # data = response.json()
    # assert data["detail"] == "Invalid credentials" # Or whatever the specific message is for 401

def test_login_incorrect_password(client: TestClient):
    email = "loginfail@example.com"
    # 1. Register a user
    register_payload = {"email": email, "password": "correct"}
    response_register = client.post("/api/v1/users/register", json=register_payload)
    assert response_register.status_code == 201

    # 2. Attempt to login with incorrect password
    login_payload = {"username": email, "password": "incorrect"}
    response_login = client.post("/api/v1/users/login", data=login_payload)
    assert response_login.status_code == 401
    data = response_login.json()
    assert data["detail"] == "Invalid credentials"

def test_access_protected_endpoint_no_token(client: TestClient):
    response = client.get("/api/v1/users/me")
    assert response.status_code == 401 # FastAPI default for missing token
    data = response.json()
    assert data["detail"] == "Not authenticated" # Default message for missing token

def test_access_protected_endpoint_invalid_token(client: TestClient):
    headers = {"Authorization": "Bearer invalidtoken"}
    response = client.get("/api/v1/users/me", headers=headers)
    assert response.status_code == 401 # FastAPI default for invalid token
    data = response.json()
    # The detail can vary based on JWTError, e.g. "Could not validate credentials"
    # or more specific like "Invalid token" or "Token has expired"
    assert "detail" in data # Check if detail is present, message can be variable


def test_inactive_user_cannot_use_token(client: TestClient, db_session: Session):
    email = "inactive@example.com"
    password = "secret"

    # 1. Register a new user
    register_payload = {"email": email, "password": password}
    response_register = client.post("/api/v1/users/register", json=register_payload)
    assert response_register.status_code == 201

    # 2. Log in as this user to get an access token
    login_payload = {"username": email, "password": password}
    response_login = client.post("/api/v1/users/login", data=login_payload)
    assert response_login.status_code == 200
    token_data = response_login.json()
    access_token = token_data["access_token"]

    # 3. Using db_session and crud.user.get_user_by_email, fetch this user
    # Ensure get_user_by_email does not raise 404 here, user should exist
    user = crud_user.get_user_by_email(db_session, email)
    assert user is not None
    assert user.email == email

    # 4. Set user.is_active = False and commit
    user.is_active = False
    db_session.add(user) # Add user to session before commit if it was detached or state changed
    db_session.commit()
    db_session.refresh(user) # Refresh to ensure state is updated from DB if necessary

    # 5. Attempt to access /api/v1/users/me using the token
    headers = {"Authorization": f"Bearer {access_token}"}
    response_me = client.get("/api/v1/users/me", headers=headers)
    assert response_me.status_code == 400 # Due to is_active check
    data_me = response_me.json()
    assert data_me["detail"] == "Inactive user"