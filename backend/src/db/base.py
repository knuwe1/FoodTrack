# backend/src/db/base.py

# Import geändert gemäß SQLAlchemy 2.0 Warnung
from sqlalchemy.orm import declarative_base

# Basis-Klasse für alle ORM-Modelle
Base = declarative_base()