# backend/tests/conftest.py

import sys
import os
from pathlib import Path

# Entferne RUNNING_TESTS - nicht mehr benötigt
# if "RUNNING_TESTS" in os.environ:
#     del os.environ["RUNNING_TESTS"]

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

# 1) Pfad anpassen
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

# 2) Imports
from src.db.base import Base
from src.db.session import get_db
from src.main import app # Importiere die App
# Modelle importieren, damit Base sie kennt
from src.models.lebensmittel import Lebensmittel
from src.models.user import User

# 3) Test-Datenbank Engine (bleibt bestehen, aber DB wird pro Test neu erstellt)
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
# connect_args nur nötig, wenn nicht default SQLite verwendet wird oder spezielle Args nötig sind
# Bei :memory: kann es manchmal helfen, eine feste Engine über Tests hinweg zu haben,
# aber wir erstellen Tabellen jetzt pro Test.
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    # WICHTIG für SQLite mit TestClient: check_same_thread=False
    connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(
    autocommit=False, autoflush=False, bind=engine
)


# 4) Function-Scoped Fixture für eine saubere DB-Session PRO TEST
@pytest.fixture(scope="function")
def db_session() -> Session:
    # Verbindung & Transaktion erstellen
    connection = engine.connect()
    transaction = connection.begin()
    # Session an die Transaktion binden
    db = TestingSessionLocal(bind=connection)

    # Tabellen erstellen
    Base.metadata.create_all(bind=engine) # Erstellt Tabellen für diesen Test

    yield db # Session für den Test bereitstellen

    # Nach dem Test:
    db.close()
    transaction.rollback() # Änderungen zurückrollen
    connection.close()
    # Optional: Tabellen wieder löschen (kann sauberer sein, aber :memory: wird eh verworfen)
    # Base.metadata.drop_all(bind=engine)


# 5) Function-Scoped Fixture zum Überschreiben der DB-Abhängigkeit PRO TEST
@pytest.fixture(scope="function")
def override_get_db(db_session: Session):
    # Definiere die Override-Funktion, die die aktuelle Test-Session zurückgibt
    def _override_get_db():
        yield db_session

    # Wende den Override auf die App an
    app.dependency_overrides[get_db] = _override_get_db
    yield # Lasse den Test laufen
    # Entferne den Override nach dem Test
    del app.dependency_overrides[get_db]


# 6) Function-Scoped Test Client
@pytest.fixture(scope="function")
def client(override_get_db): # Hängt vom DB override ab
    # Erstellt einen neuen Client für jeden Test, der die überschriebene DB verwendet
    with TestClient(app) as c:
        yield c