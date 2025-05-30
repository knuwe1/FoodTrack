# backend/src/models/transaction.py

from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum as SQLEnum
from sqlalchemy.orm import relationship
from datetime import datetime
import enum

from src.db.base import Base


class TransactionType(enum.Enum):
    PURCHASE = "PURCHASE"      # Einkauf - Menge hinzufügen
    CONSUMPTION = "CONSUMPTION"  # Verbrauch - Menge reduzieren
    ADJUSTMENT = "ADJUSTMENT"   # Korrektur - Menge anpassen
    EXPIRED = "EXPIRED"        # Ablauf - Menge entfernen


class Transaction(Base):
    __tablename__ = "transactions"

    id = Column(Integer, primary_key=True, index=True)
    lebensmittel_id = Column(Integer, ForeignKey("lebensmittel.id"), nullable=False)
    batch_id = Column(Integer, ForeignKey("lebensmittel_batches.id"), nullable=True)  # Referenz zur Batch
    transaction_type = Column(SQLEnum(TransactionType), nullable=False)
    quantity_change = Column(Integer, nullable=False)  # Positive für Zugang, negative für Abgang
    quantity_before = Column(Integer, nullable=True)   # Menge vor der Transaktion
    quantity_after = Column(Integer, nullable=True)    # Menge nach der Transaktion
    reason = Column(String(255), nullable=True)        # Grund für die Transaktion
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    # Relationships
    lebensmittel = relationship("Lebensmittel", back_populates="transactions")
    batch = relationship("LebensmittelBatch", foreign_keys=[batch_id])
