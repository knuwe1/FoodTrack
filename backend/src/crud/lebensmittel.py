# backend/src/crud/lebensmittel.py

from typing import List, Optional
from sqlalchemy.orm import Session
from src.models.lebensmittel import Lebensmittel
from src.schemas.lebensmittel import LebensmittelCreate, LebensmittelUpdate

# Umbenannt für Klarheit
def get_lebensmittel_list(db: Session, skip: int = 0, limit: int = 100) -> List[Lebensmittel]:
    return db.query(Lebensmittel).offset(skip).limit(limit).all()

# Behält den ursprünglichen Namen bei, da er im Endpoint verwendet wird,
# stellt aber sicher, dass nach ID gefiltert wird.
def get_lebensmittel(db: Session, item_id: int) -> Optional[Lebensmittel]:
    return db.query(Lebensmittel).filter(Lebensmittel.id == item_id).first()

def create_lebensmittel(db: Session, lebensmittel_in: LebensmittelCreate) -> Lebensmittel:
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
    db.delete(db_item)
    db.commit()
    # Gibt nichts zurück, da das Objekt gelöscht wurde