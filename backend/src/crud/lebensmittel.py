# backend/src/crud/lebensmittel.py

from typing import List, Optional
from datetime import datetime
from fastapi import HTTPException
from sqlalchemy.orm import Session, joinedload
from src.models.lebensmittel import Lebensmittel
from src.models.batch import LebensmittelBatch
from src.schemas.lebensmittel import LebensmittelCreate, LebensmittelUpdate

# Umbenannt für Klarheit
def get_lebensmittel_list(db: Session, skip: int = 0, limit: int = 100) -> List[Lebensmittel]:
    """
    Retrieves a list of Lebensmittel items with pagination.

    Args:
        db: The database session.
        skip: The number of items to skip.
        limit: The maximum number of items to return.

    Returns:
        A list of Lebensmittel model instances.
    """
    lebensmittel_list = db.query(Lebensmittel).options(joinedload(Lebensmittel.batches)).offset(skip).limit(limit).all()

    # Berechne echte Mengen aus Batches
    for lm in lebensmittel_list:
        lm._batches_loaded = True
        # Überschreibe die Legacy-Menge mit der echten Batch-Summe
        lm.menge = sum(batch.menge for batch in lm.batches if batch.menge > 0)

    return lebensmittel_list

# Behält den ursprünglichen Namen bei, da er im Endpoint verwendet wird,
# stellt aber sicher, dass nach ID gefiltert wird.
def get_lebensmittel(db: Session, item_id: int) -> Lebensmittel:
    """
    Retrieves a specific Lebensmittel item by its ID.

    Args:
        db: The database session.
        item_id: The ID of the Lebensmittel item to retrieve.

    Returns:
        The Lebensmittel model instance if found.

    Raises:
        HTTPException: If no Lebensmittel item with the given ID is found (status_code 404).
    """
    db_lebensmittel = db.query(Lebensmittel).options(joinedload(Lebensmittel.batches)).filter(Lebensmittel.id == item_id).first()
    if db_lebensmittel is None:
        raise HTTPException(status_code=404, detail="Lebensmittel not found")

    # Berechne echte Menge aus Batches
    db_lebensmittel._batches_loaded = True
    db_lebensmittel.menge = sum(batch.menge for batch in db_lebensmittel.batches if batch.menge > 0)

    return db_lebensmittel

def create_lebensmittel(db: Session, lebensmittel_in: LebensmittelCreate) -> Lebensmittel:
    """
    Creates a new Lebensmittel item in the database.
    If menge > 0, creates an initial batch with the specified quantity and expiration date.

    Args:
        db: The database session.
        lebensmittel_in: The data for the new Lebensmittel item, based on LebensmittelCreate schema.

    Returns:
        The newly created Lebensmittel model instance.
    """
    # Erstelle Lebensmittel ohne initiale Menge (wird durch Batches berechnet)
    db_item = Lebensmittel(
        name=lebensmittel_in.name,
        menge=0,  # Wird durch Batches berechnet
        einheit=lebensmittel_in.einheit,
        ablaufdatum=lebensmittel_in.ablaufdatum,
        kategorie=lebensmittel_in.kategorie,
        ean_code=lebensmittel_in.ean_code,
        mindestmenge=lebensmittel_in.mindestmenge or 0,
    )
    db.add(db_item)
    db.commit()
    db.refresh(db_item)

    # Erstelle initiale Batch, wenn Menge > 0
    if lebensmittel_in.menge and lebensmittel_in.menge > 0:
        initial_batch = LebensmittelBatch(
            lebensmittel_id=db_item.id,
            menge=lebensmittel_in.menge,
            ablaufdatum=lebensmittel_in.ablaufdatum
        )
        db.add(initial_batch)
        db.commit()

        # Aktualisiere die berechnete Menge
        db.refresh(db_item)
        db_item.menge = lebensmittel_in.menge

    return db_item

def get_lebensmittel_by_ean(db: Session, ean_code: str) -> Optional[Lebensmittel]:
    """
    Findet ein Lebensmittel anhand des EAN-Codes.

    Args:
        db: The database session.
        ean_code: Der EAN-Code zum Suchen.

    Returns:
        Das Lebensmittel-Objekt oder None wenn nicht gefunden.
    """
    if not ean_code:
        return None

    db_lebensmittel = db.query(Lebensmittel).options(joinedload(Lebensmittel.batches)).filter(
        Lebensmittel.ean_code == ean_code
    ).first()

    if db_lebensmittel:
        # Berechne echte Menge aus Batches
        db_lebensmittel._batches_loaded = True
        db_lebensmittel.menge = sum(batch.menge for batch in db_lebensmittel.batches if batch.menge > 0)

    return db_lebensmittel

def get_lebensmittel_below_minimum(db: Session) -> List[Lebensmittel]:
    """
    Findet alle Lebensmittel, die unter ihrer Mindestmenge liegen.

    Args:
        db: The database session.

    Returns:
        Liste der Lebensmittel unter Mindestmenge.
    """
    lebensmittel_list = db.query(Lebensmittel).options(joinedload(Lebensmittel.batches)).filter(
        Lebensmittel.mindestmenge > 0
    ).all()

    # Berechne echte Mengen und filtere
    result = []
    for item in lebensmittel_list:
        item._batches_loaded = True
        item.menge = sum(batch.menge for batch in item.batches if batch.menge > 0)
        if item.is_below_minimum:
            result.append(item)

    return result

# Akzeptiert db_item direkt und verwendet LebensmittelUpdate
def update_lebensmittel(
    db: Session, db_item: Lebensmittel, lebensmittel_in: LebensmittelUpdate
) -> Optional[Lebensmittel]:
    """
    Updates an existing Lebensmittel item in the database.

    Args:
        db: The database session.
        db_item: The existing Lebensmittel model instance to update.
        lebensmittel_in: The new data for the Lebensmittel item, based on LebensmittelUpdate schema.
                         Only fields present in lebensmittel_in will be updated.

    Returns:
        The updated Lebensmittel model instance, or None if the item was not found (though current implementation
        assumes db_item is a valid, existing item).
    """
    # Wandle das Update-Schema in ein Dict um, schließe nicht gesetzte Werte aus
    update_data = lebensmittel_in.model_dump(exclude_unset=True, by_alias=False) # by_alias=False, um Feldnamen zu verwenden

    # Aktualisiere die Felder des db_item
    for field, value in update_data.items():
        setattr(db_item, field, value)

    db.add(db_item) # Fügt das Objekt zur Session hinzu (nötig bei Änderungen)
    db.commit()
    db.refresh(db_item)
    return db_item

# Akzeptiert db_item direkt
def delete_lebensmittel(db: Session, db_item: Lebensmittel) -> None:
    """
    Deletes a Lebensmittel item from the database.

    Args:
        db: The database session.
        db_item: The Lebensmittel model instance to delete.

    Returns:
        None.

    Side effects:
        Commits the transaction to the database.
    """
    db.delete(db_item)
    db.commit()
    # Gibt nichts zurück, da das Objekt gelöscht wurde