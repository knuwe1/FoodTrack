from sqlalchemy.orm import Session
from sqlalchemy import and_, desc
from typing import List, Optional
from datetime import date, datetime

from ..models.batch import LebensmittelBatch
from ..models.lebensmittel import Lebensmittel
from ..models.transaction import Transaction, TransactionType
from ..schemas.batch import (
    LebensmittelBatchCreate, 
    LebensmittelBatchUpdate,
    PurchaseRequest,
    ConsumptionRequest,
    BatchTransactionResult
)

def create_batch(db: Session, batch: LebensmittelBatchCreate) -> LebensmittelBatch:
    """Erstellt eine neue Lebensmittel-Batch"""
    db_batch = LebensmittelBatch(**batch.dict())
    db.add(db_batch)
    db.commit()
    db.refresh(db_batch)
    return db_batch

def get_batch(db: Session, batch_id: int) -> Optional[LebensmittelBatch]:
    """Holt eine Batch anhand der ID"""
    return db.query(LebensmittelBatch).filter(LebensmittelBatch.id == batch_id).first()

def get_batches_by_lebensmittel(db: Session, lebensmittel_id: int) -> List[LebensmittelBatch]:
    """Holt alle Batches eines Lebensmittels, sortiert nach Ablaufdatum (FIFO)"""
    return db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.lebensmittel_id == lebensmittel_id,
            LebensmittelBatch.menge > 0
        )
    ).order_by(
        LebensmittelBatch.ablaufdatum.asc().nulls_last(),
        LebensmittelBatch.created_at.asc()
    ).all()

def update_batch(db: Session, batch_id: int, batch_update: LebensmittelBatchUpdate) -> Optional[LebensmittelBatch]:
    """Aktualisiert eine Batch"""
    db_batch = get_batch(db, batch_id)
    if not db_batch:
        return None
    
    update_data = batch_update.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_batch, field, value)
    
    db.commit()
    db.refresh(db_batch)
    return db_batch

def delete_batch(db: Session, batch_id: int) -> bool:
    """Löscht eine Batch (nur wenn Menge = 0)"""
    db_batch = get_batch(db, batch_id)
    if not db_batch or db_batch.menge > 0:
        return False
    
    db.delete(db_batch)
    db.commit()
    return True

def purchase_lebensmittel(db: Session, lebensmittel_id: int, purchase: PurchaseRequest) -> BatchTransactionResult:
    """
    Führt einen Einkauf durch - erstellt neue Batch mit eigenem MHD
    """
    # Prüfe ob Lebensmittel existiert
    lebensmittel = db.query(Lebensmittel).filter(Lebensmittel.id == lebensmittel_id).first()
    if not lebensmittel:
        raise ValueError(f"Lebensmittel mit ID {lebensmittel_id} nicht gefunden")
    
    # Erstelle neue Batch
    new_batch = LebensmittelBatch(
        lebensmittel_id=lebensmittel_id,
        menge=purchase.menge,
        ablaufdatum=purchase.ablaufdatum,
        einkaufsdatum=date.today()
    )
    db.add(new_batch)
    db.flush()  # Um ID zu bekommen
    
    # Erstelle Transaction
    transaction = Transaction(
        lebensmittel_id=lebensmittel_id,
        batch_id=new_batch.id,
        transaction_type=TransactionType.PURCHASE,
        quantity_change=purchase.menge,
        quantity_before=0,
        quantity_after=purchase.menge,
        reason=purchase.grund
    )
    db.add(transaction)
    db.commit()
    db.refresh(new_batch)
    db.refresh(transaction)
    
    # Berechne neue Gesamtmenge
    total_menge = sum(batch.menge for batch in lebensmittel.batches if batch.menge > 0)
    
    return BatchTransactionResult(
        transaction_id=transaction.id,
        affected_batches=[new_batch.id],
        total_quantity_change=purchase.menge,
        remaining_total=total_menge,
        message=f"Einkauf von {purchase.menge} {lebensmittel.einheit or 'Stück'} erfolgreich. Neue Batch erstellt."
    )

def consume_lebensmittel(db: Session, lebensmittel_id: int, consumption: ConsumptionRequest) -> BatchTransactionResult:
    """
    Führt einen Verbrauch durch - FIFO aus ältesten Batches
    """
    # Prüfe ob Lebensmittel existiert
    lebensmittel = db.query(Lebensmittel).filter(Lebensmittel.id == lebensmittel_id).first()
    if not lebensmittel:
        raise ValueError(f"Lebensmittel mit ID {lebensmittel_id} nicht gefunden")
    
    # Hole verfügbare Batches (FIFO-sortiert)
    if consumption.batch_id:
        # Spezifische Batch
        batches = [get_batch(db, consumption.batch_id)]
        if not batches[0] or batches[0].lebensmittel_id != lebensmittel_id:
            raise ValueError(f"Batch {consumption.batch_id} nicht gefunden oder gehört nicht zu diesem Lebensmittel")
    else:
        # FIFO: Älteste Batches zuerst
        batches = get_batches_by_lebensmittel(db, lebensmittel_id)
    
    # Prüfe verfügbare Gesamtmenge
    total_available = sum(batch.menge for batch in batches if batch.menge > 0)
    if total_available < consumption.menge:
        raise ValueError(f"Nicht genügend Menge verfügbar. Verfügbar: {total_available}, Angefordert: {consumption.menge}")
    
    # Verbrauche aus Batches (FIFO)
    remaining_to_consume = consumption.menge
    affected_batches = []
    transactions = []
    
    for batch in batches:
        if remaining_to_consume <= 0:
            break
            
        if batch.menge <= 0:
            continue
            
        # Berechne Verbrauch aus dieser Batch
        consume_from_batch = min(batch.menge, remaining_to_consume)
        quantity_before = batch.menge
        batch.menge -= consume_from_batch
        quantity_after = batch.menge
        
        # Erstelle Transaction für diese Batch
        transaction = Transaction(
            lebensmittel_id=lebensmittel_id,
            batch_id=batch.id,
            transaction_type=TransactionType.CONSUMPTION,
            quantity_change=-consume_from_batch,
            quantity_before=quantity_before,
            quantity_after=quantity_after,
            reason=consumption.grund
        )
        db.add(transaction)
        transactions.append(transaction)
        affected_batches.append(batch.id)
        
        remaining_to_consume -= consume_from_batch
    
    db.commit()
    
    # Berechne neue Gesamtmenge
    total_menge = sum(batch.menge for batch in lebensmittel.batches if batch.menge > 0)
    
    return BatchTransactionResult(
        transaction_id=transactions[0].id if transactions else 0,
        affected_batches=affected_batches,
        total_quantity_change=-consumption.menge,
        remaining_total=total_menge,
        message=f"Verbrauch von {consumption.menge} {lebensmittel.einheit or 'Stück'} erfolgreich aus {len(affected_batches)} Batch(es)."
    )

def get_expired_batches(db: Session) -> List[LebensmittelBatch]:
    """Holt alle abgelaufenen Batches"""
    today = date.today()
    return db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.ablaufdatum < today,
            LebensmittelBatch.menge > 0
        )
    ).all()

def get_expiring_soon_batches(db: Session, days_threshold: int = 3) -> List[LebensmittelBatch]:
    """Holt alle bald ablaufenden Batches"""
    from datetime import timedelta
    threshold_date = date.today() + timedelta(days=days_threshold)
    
    return db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.ablaufdatum <= threshold_date,
            LebensmittelBatch.ablaufdatum >= date.today(),
            LebensmittelBatch.menge > 0
        )
    ).all()
