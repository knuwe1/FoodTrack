# backend/src/schemas/transaction.py

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict
from enum import Enum


class TransactionType(str, Enum):
    PURCHASE = "PURCHASE"
    CONSUMPTION = "CONSUMPTION"
    ADJUSTMENT = "ADJUSTMENT"
    EXPIRED = "EXPIRED"


class TransactionBase(BaseModel):
    lebensmittel_id: int
    transaction_type: TransactionType
    quantity_change: int = Field(..., description="Positive for additions, negative for reductions")
    reason: Optional[str] = None


class TransactionCreate(TransactionBase):
    pass


class TransactionUpdate(BaseModel):
    reason: Optional[str] = None

    model_config = ConfigDict(
        populate_by_name=True
    )


class TransactionRead(TransactionBase):
    id: int
    quantity_before: Optional[int] = None
    quantity_after: Optional[int] = None
    created_at: datetime

    model_config = ConfigDict(
        from_attributes=True,
        populate_by_name=True
    )


class TransactionWithLebensmittel(TransactionRead):
    lebensmittel_name: str
    lebensmittel_kategorie: Optional[str] = None


# Statistics schemas
class CategoryStatistics(BaseModel):
    kategorie: str
    total_purchases: int
    total_consumption: int
    net_change: int
    transaction_count: int


class MonthlyStatistics(BaseModel):
    year: int
    month: int
    total_purchases: int
    total_consumption: int
    net_change: int
    transaction_count: int


class LebensmittelStatistics(BaseModel):
    lebensmittel_id: int
    lebensmittel_name: str
    kategorie: Optional[str] = None
    total_purchases: int
    total_consumption: int
    net_change: int
    transaction_count: int
    last_transaction: Optional[datetime] = None


class StatisticsOverview(BaseModel):
    total_transactions: int
    total_purchases: int
    total_consumption: int
    most_purchased_category: Optional[str] = None
    most_consumed_category: Optional[str] = None
    categories: list[CategoryStatistics]
    monthly: list[MonthlyStatistics]
    top_items: list[LebensmittelStatistics]
