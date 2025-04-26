# backend/src/models/lebensmittel.py

from sqlalchemy import Column, Integer, String, Date, Boolean # Boolean hinzugefügt
from src.db.base import Base

class Lebensmittel(Base):
    __tablename__ = "lebensmittel"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    menge = Column(Integer, nullable=False)
    # Geändert zu nullable=True, um mit dem Optional[str] im Schema übereinzustimmen
    einheit = Column(String, nullable=True)
    ablaufdatum = Column(Date, nullable=True)
    kategorie = Column(String, nullable=True)