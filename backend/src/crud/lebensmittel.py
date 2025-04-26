from sqlalchemy.orm import Session
from src.models.lebensmittel import Lebensmittel
from src.schemas.lebensmittel import LebensmittelCreate

def get_lebensmittel(db: Session, skip: int = 0, limit: int = 100):
    return db.query(Lebensmittel).offset(skip).limit(limit).all()

def get_lebensmittel_by_id(db: Session, item_id: int):
    return db.query(Lebensmittel).filter(Lebensmittel.id == item_id).first()

def create_lebensmittel(db: Session, lebensmittel_in: LebensmittelCreate):
    db_item = Lebensmittel(
        name=lebensmittel_in.name,
        menge=lebensmittel_in.quantity,
        einheit=lebensmittel_in.unit,
        ablaufdatum=lebensmittel_in.expiry_date,
        kategorie=lebensmittel_in.category,
    )
    db.add(db_item)
    db.commit()
    db.refresh(db_item)
    return db_item

def update_lebensmittel(db: Session, item_id: int, lebensmittel_in: LebensmittelCreate):
    db_item = get_lebensmittel_by_id(db, item_id)
    if not db_item:
        return None
    db_item.name = lebensmittel_in.name
    db_item.menge = lebensmittel_in.quantity
    db_item.einheit = lebensmittel_in.unit
    db_item.ablaufdatum = lebensmittel_in.expiry_date
    db_item.kategorie = lebensmittel_in.category
    db.commit()
    db.refresh(db_item)
    return db_item

def delete_lebensmittel(db: Session, item_id: int):
    db_item = get_lebensmittel_by_id(db, item_id)
    if not db_item:
        return None
    db.delete(db_item)
    db.commit()
    return db_item
