from pydantic import BaseModel, Field
from typing import Optional
from datetime import date, datetime

class LebensmittelBatchBase(BaseModel):
    menge: int = Field(..., ge=0, description="Menge der Batch")
    ablaufdatum: Optional[date] = Field(None, description="Mindesthaltbarkeitsdatum")
    einkaufsdatum: Optional[date] = Field(None, description="Einkaufsdatum")

class LebensmittelBatchCreate(LebensmittelBatchBase):
    lebensmittel_id: int = Field(..., description="ID des Lebensmittels")

class LebensmittelBatchUpdate(BaseModel):
    menge: Optional[int] = Field(None, ge=0, description="Neue Menge")
    ablaufdatum: Optional[date] = Field(None, description="Neues MHD")

class LebensmittelBatch(LebensmittelBatchBase):
    id: int
    lebensmittel_id: int
    einkaufsdatum: date
    created_at: datetime
    updated_at: datetime
    
    # Computed properties
    is_expired: bool = Field(..., description="Ist die Batch abgelaufen?")
    days_until_expiry: int = Field(..., description="Tage bis zum Ablauf")
    is_expiring_soon: bool = Field(..., description="Läuft die Batch bald ab?")

    class Config:
        from_attributes = True

class LebensmittelBatchSummary(BaseModel):
    """Zusammenfassung aller Batches eines Lebensmittels"""
    lebensmittel_id: int
    total_menge: int = Field(..., description="Gesamtmenge aller Batches")
    batch_count: int = Field(..., description="Anzahl der Batches")
    earliest_expiry: Optional[date] = Field(None, description="Frühestes Ablaufdatum")
    expired_batches: int = Field(..., description="Anzahl abgelaufener Batches")
    expiring_soon_batches: int = Field(..., description="Anzahl bald ablaufender Batches")
    batches: list[LebensmittelBatch] = Field(..., description="Liste aller Batches")

# Purchase-spezifische Schemas
class PurchaseRequest(BaseModel):
    menge: int = Field(..., gt=0, description="Eingekaufte Menge")
    ablaufdatum: Optional[date] = Field(None, description="MHD der neuen Batch")
    grund: Optional[str] = Field(None, max_length=255, description="Grund für den Einkauf")

class ConsumptionRequest(BaseModel):
    menge: int = Field(..., gt=0, description="Verbrauchte Menge")
    grund: Optional[str] = Field(None, max_length=255, description="Grund für den Verbrauch")
    # Optional: Spezifische Batch-ID für manuellen Verbrauch
    batch_id: Optional[int] = Field(None, description="Spezifische Batch für Verbrauch (sonst FIFO)")

class BatchTransactionResult(BaseModel):
    """Ergebnis einer Batch-Transaktion"""
    transaction_id: int
    affected_batches: list[int] = Field(..., description="IDs der betroffenen Batches")
    total_quantity_change: int = Field(..., description="Gesamte Mengenänderung")
    remaining_total: int = Field(..., description="Verbleibende Gesamtmenge")
    message: str = Field(..., description="Beschreibung der Transaktion")
