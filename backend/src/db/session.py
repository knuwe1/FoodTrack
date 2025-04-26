# backend/src/db/session.py

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from .base import Base
from ..config import settings

# Engine erzeugen
# Die Option connect_args={"check_same_thread": False} wurde entfernt,
# da sie spezifisch für SQLite ist und für PostgreSQL nicht benötigt wird.
engine = create_engine(settings.DATABASE_URL)

# Session-Klasse
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

# Hilfsfunktion für Dependency Injection in FastAPI
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Tabellen erstellen (einmalig beim Start)
def init_db():
    # Stelle sicher, dass alle Modelle importiert sind, bevor create_all aufgerufen wird.
    # Dies kann durch Importieren der Modelle hier oder in db.base geschehen.
    from ..models.lebensmittel import Lebensmittel # Beispielimport
    from ..models.user import User # Beispielimport
    Base.metadata.create_all(bind=engine)