# backend/tests/conftest.py

import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# 1) Projekt-Root auf den Modulpfad packen
ROOT = Path(__file__).resolve().parent.parent  # …/FoodTrack/backend
sys.path.insert(0, str(ROOT))

# 2) Imports erst NACH dem Pfad-Hack
from src.db.base import Base
from src.db.session import get_db
from src.main import app

# 3) In-Memory-SQLite für Tests
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(
    autocommit=False, autoflush=False, bind=engine
)

# 4) Tabellen einmal anlegen
Base.metadata.create_all(bind=engine)

# 5) get_db override
def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

# 6) Test-Client
@pytest.fixture(scope="module")
def client():
    return TestClient(app)
