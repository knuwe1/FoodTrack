from sqlalchemy import Column, Integer, String, Date, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from src.db.base import Base
import datetime

class LebensmittelBatch(Base):
    """
    Repräsentiert eine Charge/Batch eines Lebensmittels mit eigenem MHD
    """
    __tablename__ = "lebensmittel_batches"

    id = Column(Integer, primary_key=True, index=True)
    lebensmittel_id = Column(Integer, ForeignKey("lebensmittel.id"), nullable=False)
    menge = Column(Integer, nullable=False, default=0)
    ablaufdatum = Column(Date, nullable=True)
    einkaufsdatum = Column(Date, nullable=False, default=datetime.date.today)
    created_at = Column(DateTime, nullable=False, default=func.now())
    updated_at = Column(DateTime, nullable=False, default=func.now(), onupdate=func.now())

    # Relationships
    lebensmittel = relationship("Lebensmittel", back_populates="batches")
    transactions = relationship("Transaction", foreign_keys="Transaction.batch_id")

    def __repr__(self):
        return f"<LebensmittelBatch(id={self.id}, lebensmittel_id={self.lebensmittel_id}, menge={self.menge}, ablaufdatum={self.ablaufdatum})>"

    @property
    def is_expired(self) -> bool:
        """Prüft ob die Batch abgelaufen ist"""
        if not self.ablaufdatum:
            return False
        return self.ablaufdatum < datetime.date.today()

    @property
    def days_until_expiry(self) -> int:
        """Berechnet Tage bis zum Ablauf (negativ = bereits abgelaufen)"""
        if not self.ablaufdatum:
            return 999  # Kein MHD = sehr lange haltbar
        delta = self.ablaufdatum - datetime.date.today()
        return delta.days

    @property
    def is_expiring_soon(self, days_threshold: int = 3) -> bool:
        """Prüft ob die Batch bald abläuft"""
        return 0 <= self.days_until_expiry <= days_threshold
