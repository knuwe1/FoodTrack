# backend/src/db/base.py

# Import geändert gemäß SQLAlchemy 2.0 Warnung
from sqlalchemy.orm import declarative_base

# Basis-Klasse für alle ORM-Modelle
Base = declarative_base()

# Import all the models here so that Base.metadata.create_all() can find them
from src.models.lebensmittel import Lebensmittel
from src.models.user import User
from src.models.transaction import Transaction