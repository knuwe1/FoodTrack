# backend/src/crud/lebensmittel.py

from typing import List, Optional
from fastapi import HTTPException
from sqlalchemy.orm import Session
from src.models.lebensmittel import Lebensmittel
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
    return db.query(Lebensmittel).offset(skip).limit(limit).all()

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
    db_lebensmittel = db.query(Lebensmittel).filter(Lebensmittel.id == item_id).first()
    if db_lebensmittel is None:
        raise HTTPException(status_code=404, detail="Lebensmittel not found")
    return db_lebensmittel

def create_lebensmittel(db: Session, lebensmittel_in: LebensmittelCreate) -> Lebensmittel:
    """
    Creates a new Lebensmittel item in the database.

    Args:
        db: The database session.
        lebensmittel_in: The data for the new Lebensmittel item, based on LebensmittelCreate schema.

    Returns:
        The newly created Lebensmittel model instance.
    """
    # Verwende die korrekten Attributnamen aus dem Schema
    db_item = Lebensmittel(
        name=lebensmittel_in.name,
        menge=lebensmittel_in.menge, # Nicht lebensmittel_in.quantity
        einheit=lebensmittel_in.einheit,
        ablaufdatum=lebensmittel_in.ablaufdatum,
        kategorie=lebensmittel_in.kategorie,
    )
    db.add(db_item)
    db.commit()
    db.refresh(db_item)
    return db_item

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