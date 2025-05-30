from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import date, timedelta

from ...database import get_db
from ...models.batch import LebensmittelBatch
from ...models.lebensmittel import Lebensmittel
from sqlalchemy import and_, or_

router = APIRouter()

@router.get("/expired")
def get_expired_batches(
    db: Session = Depends(get_db)
):
    """
    Holt alle abgelaufenen Batches
    """
    today = date.today()

    expired_batches = db.query(LebensmittelBatch).join(Lebensmittel).filter(
        and_(
            LebensmittelBatch.ablaufdatum < today,
            LebensmittelBatch.menge > 0
        )
    ).all()

    result = []
    for batch in expired_batches:
        days_expired = (today - batch.ablaufdatum).days if batch.ablaufdatum else 0
        result.append({
            "batch_id": batch.id,
            "lebensmittel_id": batch.lebensmittel_id,
            "lebensmittel_name": batch.lebensmittel.name,
            "menge": batch.menge,
            "einheit": batch.lebensmittel.einheit,
            "ablaufdatum": batch.ablaufdatum,
            "days_expired": days_expired,
            "einkaufsdatum": batch.einkaufsdatum
        })

    return {
        "count": len(result),
        "expired_batches": result
    }

@router.get("/expiring-soon")
def get_expiring_soon_batches(
    days_threshold: int = Query(3, description="Anzahl Tage für 'bald ablaufend'"),
    db: Session = Depends(get_db)
):
    """
    Holt alle bald ablaufenden Batches
    """
    today = date.today()
    threshold_date = today + timedelta(days=days_threshold)

    expiring_batches = db.query(LebensmittelBatch).join(Lebensmittel).filter(
        and_(
            LebensmittelBatch.ablaufdatum <= threshold_date,
            LebensmittelBatch.ablaufdatum >= today,
            LebensmittelBatch.menge > 0
        )
    ).order_by(LebensmittelBatch.ablaufdatum.asc()).all()

    result = []
    for batch in expiring_batches:
        days_until_expiry = (batch.ablaufdatum - today).days if batch.ablaufdatum else 999
        result.append({
            "batch_id": batch.id,
            "lebensmittel_id": batch.lebensmittel_id,
            "lebensmittel_name": batch.lebensmittel.name,
            "menge": batch.menge,
            "einheit": batch.lebensmittel.einheit,
            "ablaufdatum": batch.ablaufdatum,
            "days_until_expiry": days_until_expiry,
            "einkaufsdatum": batch.einkaufsdatum,
            "urgency": "critical" if days_until_expiry <= 1 else "warning" if days_until_expiry <= 2 else "info"
        })

    return {
        "count": len(result),
        "days_threshold": days_threshold,
        "expiring_soon_batches": result
    }

@router.get("/summary")
def get_expiry_summary(
    days_threshold: int = Query(3, description="Anzahl Tage für 'bald ablaufend'"),
    db: Session = Depends(get_db)
):
    """
    Übersicht über alle Ablauf-Warnungen
    """
    today = date.today()
    threshold_date = today + timedelta(days=days_threshold)

    # Abgelaufene Batches
    expired_count = db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.ablaufdatum < today,
            LebensmittelBatch.menge > 0
        )
    ).count()

    # Bald ablaufende Batches
    expiring_count = db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.ablaufdatum <= threshold_date,
            LebensmittelBatch.ablaufdatum >= today,
            LebensmittelBatch.menge > 0
        )
    ).count()

    # Kritische Batches (heute oder morgen)
    critical_date = today + timedelta(days=1)
    critical_count = db.query(LebensmittelBatch).filter(
        and_(
            LebensmittelBatch.ablaufdatum <= critical_date,
            LebensmittelBatch.ablaufdatum >= today,
            LebensmittelBatch.menge > 0
        )
    ).count()

    # Gesamte Batches
    total_batches = db.query(LebensmittelBatch).filter(
        LebensmittelBatch.menge > 0
    ).count()

    return {
        "summary": {
            "total_batches": total_batches,
            "expired_batches": expired_count,
            "expiring_soon_batches": expiring_count,
            "critical_batches": critical_count,
            "days_threshold": days_threshold
        },
        "alerts": {
            "has_expired": expired_count > 0,
            "has_expiring_soon": expiring_count > 0,
            "has_critical": critical_count > 0,
            "total_alerts": expired_count + expiring_count
        }
    }

@router.get("/lebensmittel/{lebensmittel_id}/batches")
def get_lebensmittel_batches(
    lebensmittel_id: int,
    include_empty: bool = Query(False, description="Leere Batches einschließen"),
    db: Session = Depends(get_db)
):
    """
    Holt alle Batches eines Lebensmittels mit Ablauf-Informationen
    """
    today = date.today()

    query = db.query(LebensmittelBatch).filter(
        LebensmittelBatch.lebensmittel_id == lebensmittel_id
    )

    if not include_empty:
        query = query.filter(LebensmittelBatch.menge > 0)

    batches = query.order_by(
        LebensmittelBatch.ablaufdatum.asc().nulls_last(),
        LebensmittelBatch.created_at.asc()
    ).all()

    result = []
    for batch in batches:
        days_until_expiry = (batch.ablaufdatum - today).days if batch.ablaufdatum else 999
        is_expired = batch.ablaufdatum < today if batch.ablaufdatum else False
        is_expiring_soon = 0 <= days_until_expiry <= 3

        result.append({
            "batch_id": batch.id,
            "menge": batch.menge,
            "ablaufdatum": batch.ablaufdatum,
            "einkaufsdatum": batch.einkaufsdatum,
            "days_until_expiry": days_until_expiry,
            "is_expired": is_expired,
            "is_expiring_soon": is_expiring_soon,
            "status": "expired" if is_expired else "critical" if days_until_expiry <= 1 else "warning" if is_expiring_soon else "ok",
            "created_at": batch.created_at
        })

    return {
        "lebensmittel_id": lebensmittel_id,
        "total_batches": len(result),
        "total_menge": sum(b["menge"] for b in result),
        "batches": result
    }
