# backend/src/api/v1/endpoints/transaction.py

from typing import List, Optional
import logging
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session

from src.schemas.transaction import (
    TransactionCreate,
    TransactionRead,
    TransactionWithLebensmittel,
    StatisticsOverview,
    CategoryStatistics,
    MonthlyStatistics,
    LebensmittelStatistics,
    TransactionType
)
from src.crud.transaction import (
    create_transaction,
    get_transactions,
    get_transaction,
    get_statistics_overview,
    get_category_statistics,
    get_monthly_statistics,
    get_lebensmittel_statistics
)
from src.db.session import get_db
from src.models.transaction import Transaction as DBTransaction

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post(
    "/",
    response_model=TransactionRead,
    status_code=status.HTTP_201_CREATED,
)
def create_transaction_endpoint(
    transaction_in: TransactionCreate,
    db: Session = Depends(get_db),
) -> DBTransaction:
    """
    Erstellt eine neue Transaktion (Einkauf, Verbrauch, etc.)
    """
    logger.info(f"Creating transaction: {transaction_in.model_dump()}")
    try:
        result = create_transaction(db=db, transaction_in=transaction_in)
        logger.info(f"Created transaction with ID: {result.id}")
        return result
    except ValueError as e:
        logger.error(f"Transaction creation failed: {str(e)}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"Unexpected error creating transaction: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal server error")


@router.get(
    "/",
    response_model=List[TransactionRead],
    status_code=status.HTTP_200_OK,
)
def read_transactions(
    skip: int = 0,
    limit: int = 100,
    lebensmittel_id: Optional[int] = Query(None, description="Filter by Lebensmittel ID"),
    transaction_type: Optional[TransactionType] = Query(None, description="Filter by transaction type"),
    db: Session = Depends(get_db),
) -> List[DBTransaction]:
    """
    Holt alle Transaktionen mit optionalen Filtern
    """
    return get_transactions(
        db=db,
        skip=skip,
        limit=limit,
        lebensmittel_id=lebensmittel_id,
        transaction_type=transaction_type
    )


@router.get(
    "/{transaction_id}",
    response_model=TransactionRead,
    status_code=status.HTTP_200_OK,
)
def read_transaction(
    transaction_id: int,
    db: Session = Depends(get_db),
) -> DBTransaction:
    """
    Holt eine einzelne Transaktion
    """
    transaction = get_transaction(db=db, transaction_id=transaction_id)
    if not transaction:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return transaction


@router.get(
    "/statistics/overview",
    response_model=StatisticsOverview,
    status_code=status.HTTP_200_OK,
)
def read_statistics_overview(
    db: Session = Depends(get_db),
) -> StatisticsOverview:
    """
    Holt eine umfassende Statistik-Übersicht
    """
    return get_statistics_overview(db=db)


@router.get(
    "/statistics/categories",
    response_model=List[CategoryStatistics],
    status_code=status.HTTP_200_OK,
)
def read_category_statistics(
    db: Session = Depends(get_db),
) -> List[CategoryStatistics]:
    """
    Holt Statistiken pro Kategorie
    """
    return get_category_statistics(db=db)


@router.get(
    "/statistics/monthly",
    response_model=List[MonthlyStatistics],
    status_code=status.HTTP_200_OK,
)
def read_monthly_statistics(
    months: int = Query(12, description="Number of months to include"),
    db: Session = Depends(get_db),
) -> List[MonthlyStatistics]:
    """
    Holt monatliche Statistiken
    """
    return get_monthly_statistics(db=db, months=months)


@router.get(
    "/statistics/items",
    response_model=List[LebensmittelStatistics],
    status_code=status.HTTP_200_OK,
)
def read_lebensmittel_statistics(
    limit: int = Query(10, description="Number of top items to return"),
    db: Session = Depends(get_db),
) -> List[LebensmittelStatistics]:
    """
    Holt Statistiken pro Lebensmittel (Top Items)
    """
    return get_lebensmittel_statistics(db=db, limit=limit)


# Convenience endpoints for common actions
@router.post(
    "/purchase/{lebensmittel_id}",
    response_model=TransactionRead,
    status_code=status.HTTP_201_CREATED,
)
def record_purchase(
    lebensmittel_id: int,
    quantity: int = Query(..., description="Quantity purchased"),
    reason: Optional[str] = Query(None, description="Reason for purchase"),
    mhd: Optional[str] = Query(None, description="MHD (YYYY-MM-DD format)"),
    db: Session = Depends(get_db),
) -> DBTransaction:
    """
    Convenience endpoint für Einkäufe mit optionalem MHD

    WICHTIG: Das MHD wird aktuell nur im Grund gespeichert.
    Für echte Batch-Verwaltung ist eine Datenbank-Migration nötig.
    """
    # Für Batch-System: Erstelle neue Batch mit MHD
    from src.models.batch import LebensmittelBatch
    from datetime import date as date_type

    # Parse MHD wenn vorhanden
    mhd_date = None
    if mhd:
        try:
            mhd_date = date_type.fromisoformat(mhd)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid MHD format. Use YYYY-MM-DD")

    # Erstelle neue Batch
    new_batch = LebensmittelBatch(
        lebensmittel_id=lebensmittel_id,
        menge=quantity,
        ablaufdatum=mhd_date,
        einkaufsdatum=date_type.today()
    )
    db.add(new_batch)
    db.flush()  # Um ID zu bekommen

    # Erweitere den Grund um MHD-Information
    extended_reason = reason or "Einkauf"
    if mhd:
        extended_reason += f" (MHD: {mhd})"

    transaction_in = TransactionCreate(
        lebensmittel_id=lebensmittel_id,
        transaction_type=TransactionType.PURCHASE,
        quantity_change=quantity,
        reason=extended_reason
    )
    transaction = create_transaction(db=db, transaction_in=transaction_in)

    # Verknüpfe Transaction mit Batch (falls batch_id Feld existiert)
    if hasattr(transaction, 'batch_id'):
        transaction.batch_id = new_batch.id
        db.commit()

    return transaction


@router.post(
    "/consume/{lebensmittel_id}",
    response_model=TransactionRead,
    status_code=status.HTTP_201_CREATED,
)
def record_consumption(
    lebensmittel_id: int,
    quantity: int = Query(..., description="Quantity consumed"),
    reason: Optional[str] = Query(None, description="Reason for consumption"),
    db: Session = Depends(get_db),
) -> DBTransaction:
    """
    Convenience endpoint für Verbrauch mit FIFO-Logik

    Verbraucht automatisch aus den ältesten Batches (FIFO-Prinzip).
    """
    from src.models.batch import LebensmittelBatch
    from src.models.lebensmittel import Lebensmittel
    from sqlalchemy import and_

    # Prüfe ob Lebensmittel existiert
    lebensmittel = db.query(Lebensmittel).filter(Lebensmittel.id == lebensmittel_id).first()
    if not lebensmittel:
        raise HTTPException(status_code=404, detail="Lebensmittel not found")

    # Hole verfügbare Batches sortiert nach FIFO (älteste zuerst)
    available_batches = db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.lebensmittel_id == lebensmittel_id,
            LebensmittelBatch.menge > 0
        )
    ).order_by(
        LebensmittelBatch.ablaufdatum.asc().nulls_last(),  # Älteste MHDs zuerst
        LebensmittelBatch.created_at.asc()  # Bei gleichem MHD: älteste Batch zuerst
    ).all()

    # Prüfe verfügbare Gesamtmenge
    total_available = sum(batch.menge for batch in available_batches)
    if total_available < quantity:
        raise HTTPException(
            status_code=400,
            detail=f"Nicht genügend Menge verfügbar. Verfügbar: {total_available}, Angefordert: {quantity}"
        )

    # FIFO-Verbrauch aus Batches
    remaining_to_consume = quantity
    consumed_batches = []
    total_consumed = 0

    for batch in available_batches:
        if remaining_to_consume <= 0:
            break

        # Berechne Verbrauch aus dieser Batch
        consume_from_batch = min(batch.menge, remaining_to_consume)
        quantity_before = batch.menge

        # Reduziere Batch-Menge
        batch.menge -= consume_from_batch
        quantity_after = batch.menge

        # Erstelle Transaction für diese Batch
        transaction_in = TransactionCreate(
            lebensmittel_id=lebensmittel_id,
            transaction_type=TransactionType.CONSUMPTION,
            quantity_change=-consume_from_batch,
            reason=f"{reason or 'Verbrauch'} (Batch {batch.id}, MHD: {batch.ablaufdatum or 'kein'})"
        )

        # Erstelle Transaction ohne die normale create_transaction Funktion
        # um batch_id direkt zu setzen
        from src.models.transaction import Transaction
        transaction = Transaction(
            lebensmittel_id=transaction_in.lebensmittel_id,
            transaction_type=transaction_in.transaction_type,
            quantity_change=transaction_in.quantity_change,
            quantity_before=quantity_before,
            quantity_after=quantity_after,
            reason=transaction_in.reason,
            batch_id=batch.id  # Verknüpfe mit Batch
        )
        db.add(transaction)

        consumed_batches.append({
            'batch_id': batch.id,
            'consumed': consume_from_batch,
            'remaining': quantity_after,
            'mhd': batch.ablaufdatum
        })

        total_consumed += consume_from_batch
        remaining_to_consume -= consume_from_batch

    db.commit()

    # Gib die erste Transaction zurück (für API-Kompatibilität)
    if consumed_batches:
        # Erstelle eine zusammenfassende Transaction
        summary_transaction = Transaction(
            lebensmittel_id=lebensmittel_id,
            transaction_type=TransactionType.CONSUMPTION,
            quantity_change=-total_consumed,
            quantity_before=total_available,
            quantity_after=total_available - total_consumed,
            reason=f"{reason or 'Verbrauch'} (FIFO aus {len(consumed_batches)} Batch(es))"
        )
        db.add(summary_transaction)
        db.commit()
        db.refresh(summary_transaction)
        return summary_transaction

    raise HTTPException(status_code=500, detail="Consumption failed")
