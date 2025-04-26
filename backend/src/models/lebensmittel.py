# backend/src/models/lebensmittel.py

from sqlalchemy import Column, Integer, String, Date
from src.db.base import Base

class Lebensmittel(Base):
    __tablename__ = "lebensmittel"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    menge = Column(Integer, nullable=False)
    einheit = Column(String, nullable=False)
    ablaufdatum = Column(Date, nullable=True)
    kategorie = Column(String, nullable=True)
