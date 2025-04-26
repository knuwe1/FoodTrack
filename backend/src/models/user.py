# backend/src/models/user.py

from sqlalchemy import Column, Integer, String, Boolean
# Import geändert zu absolut (wie in lebensmittel.py)
from src.db.base import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)