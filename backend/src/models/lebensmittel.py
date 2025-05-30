# backend/src/models/lebensmittel.py

from sqlalchemy import Column, Integer, String, Date, Boolean
from sqlalchemy.orm import relationship
from src.db.base import Base

class Lebensmittel(Base):
    __tablename__ = "lebensmittel"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    menge = Column(Integer, nullable=False)  # Wird auf 0 gesetzt, echte Menge in Batches
    einheit = Column(String, nullable=True)
    ablaufdatum = Column(Date, nullable=True)  # Legacy, echte MHDs in Batches
    kategorie = Column(String, nullable=True)
    ean_code = Column(String, nullable=True, index=True)  # EAN/Barcode
    mindestmenge = Column(Integer, nullable=True, default=0)  # Mindestbestand

    # Relationships
    transactions = relationship("Transaction", back_populates="lebensmittel")
    batches = relationship("LebensmittelBatch", back_populates="lebensmittel", cascade="all, delete-orphan")

    @property
    def total_menge(self):
        """Berechnet die Gesamtmenge aus allen aktiven Batches"""
        if not hasattr(self, '_batches_loaded'):
            return self.menge  # Fallback für Legacy-Kompatibilität
        return sum(batch.menge for batch in self.batches if batch.menge > 0)

    @property
    def earliest_expiry_date(self):
        """Findet das früheste Ablaufdatum aller Batches"""
        if not hasattr(self, '_batches_loaded'):
            return self.ablaufdatum  # Fallback
        valid_dates = [batch.ablaufdatum for batch in self.batches if batch.ablaufdatum and batch.menge > 0]
        return min(valid_dates) if valid_dates else None

    @property
    def is_below_minimum(self):
        """Prüft ob der Bestand unter der Mindestmenge liegt"""
        if not self.mindestmenge or self.mindestmenge <= 0:
            return False
        current_quantity = self.total_menge if hasattr(self, '_batches_loaded') else self.menge
        return current_quantity < self.mindestmenge

    @property
    def minimum_shortage(self):
        """Berechnet wie viel fehlt bis zur Mindestmenge"""
        if not self.is_below_minimum:
            return 0
        current_quantity = self.total_menge if hasattr(self, '_batches_loaded') else self.menge
        return self.mindestmenge - current_quantity