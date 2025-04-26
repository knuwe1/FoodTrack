from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from .base import Base
from ..config import settings

# Engine erzeugen
engine = create_engine(
    settings.DATABASE_URL,
    connect_args={"check_same_thread": False}  # nur für SQLite
)

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
    Base.metadata.create_all(bind=engine)