# backend/src/crud/transaction.py

from typing import List, Optional
from sqlalchemy.orm import Session
from sqlalchemy import func, extract, desc
from datetime import datetime, timedelta

from src.models.transaction import Transaction, TransactionType
from src.models.lebensmittel import Lebensmittel
from src.schemas.transaction import (
    TransactionCreate,
    CategoryStatistics,
    MonthlyStatistics,
    LebensmittelStatistics,
    StatisticsOverview
)


def create_transaction(db: Session, transaction_in: TransactionCreate) -> Transaction:
    """
    Erstellt eine neue Transaktion und aktualisiert die Lebensmittel-Menge
    """
    # Hole das Lebensmittel
    lebensmittel = db.query(Lebensmittel).filter(Lebensmittel.id == transaction_in.lebensmittel_id).first()
    if not lebensmittel:
        raise ValueError(f"Lebensmittel with ID {transaction_in.lebensmittel_id} not found")

    # Speichere die aktuelle Menge
    quantity_before = lebensmittel.menge or 0

    # Berechne die neue Menge
    quantity_after = quantity_before + transaction_in.quantity_change

    # Verhindere negative Mengen bei Verbrauch
    if quantity_after < 0:
        raise ValueError(f"Insufficient quantity. Available: {quantity_before}, Requested: {abs(transaction_in.quantity_change)}")

    # Erstelle die Transaktion
    db_transaction = Transaction(
        lebensmittel_id=transaction_in.lebensmittel_id,
        transaction_type=transaction_in.transaction_type,
        quantity_change=transaction_in.quantity_change,
        quantity_before=quantity_before,
        quantity_after=quantity_after,
        reason=transaction_in.reason,
        created_at=datetime.utcnow()
    )

    # Aktualisiere die Lebensmittel-Menge
    lebensmittel.menge = quantity_after

    db.add(db_transaction)
    db.commit()
    db.refresh(db_transaction)

    return db_transaction


def get_transactions(
    db: Session,
    skip: int = 0,
    limit: int = 100,
    lebensmittel_id: Optional[int] = None,
    transaction_type: Optional[TransactionType] = None
) -> List[Transaction]:
    """
    Holt Transaktionen mit optionalen Filtern
    """
    query = db.query(Transaction)

    if lebensmittel_id:
        query = query.filter(Transaction.lebensmittel_id == lebensmittel_id)

    if transaction_type:
        query = query.filter(Transaction.transaction_type == transaction_type)

    return query.order_by(desc(Transaction.created_at)).offset(skip).limit(limit).all()


def get_transaction(db: Session, transaction_id: int) -> Optional[Transaction]:
    """
    Holt eine einzelne Transaktion
    """
    return db.query(Transaction).filter(Transaction.id == transaction_id).first()


def get_category_statistics(db: Session) -> List[CategoryStatistics]:
    """
    Berechnet Statistiken pro Kategorie
    """
    # Einfache Implementierung für den Start
    return []


def get_monthly_statistics(db: Session, months: int = 12) -> List[MonthlyStatistics]:
    """
    Berechnet monatliche Statistiken für die letzten X Monate
    """
    # Einfache Implementierung für den Start
    return []


def get_lebensmittel_statistics(db: Session, limit: int = 10) -> List[LebensmittelStatistics]:
    """
    Berechnet Statistiken pro Lebensmittel (Top Items)
    """
    # Einfache Implementierung für den Start
    return []


def get_statistics_overview(db: Session) -> StatisticsOverview:
    """
    Erstellt eine umfassende Statistik-Übersicht
    """
    # Einfache Implementierung für den Start (ohne Transaktionen)
    total_transactions = db.query(func.count(Transaction.id)).scalar() or 0

    # Da wir noch keine Transaktionen haben, geben wir leere Statistiken zurück
    return StatisticsOverview(
        total_transactions=total_transactions,
        total_purchases=0,
        total_consumption=0,
        most_purchased_category=None,
        most_consumed_category=None,
        categories=[],
        monthly=[],
        top_items=[]
    )
