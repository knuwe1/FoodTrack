# backend/src/api/v1/endpoints/lebensmittel.py

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from src.schemas.lebensmittel import (
    LebensmittelCreate,
    LebensmittelRead,
    LebensmittelUpdate,
)
# Importiere die CRUD-Funktionen (Namen könnten nach Refactoring angepasst sein)
from src.crud.lebensmittel import (
    create_lebensmittel,
    get_lebensmittel, # Nimmt jetzt ID
    get_lebensmittel_list, # Umbenannt im CRUD Modul
    update_lebensmittel,
    delete_lebensmittel,
)
from src.db.session import get_db
# Importiere das DB-Modell für Typ-Annotationen, falls benötigt
from src.models.lebensmittel import Lebensmittel as DBLebensmittel


router = APIRouter()

@router.post(
    "/",
    response_model=LebensmittelRead,
    # Geändert zu 201 CREATED
    status_code=status.HTTP_201_CREATED,
)
def create_item(
    lebensmittel_in: LebensmittelCreate,
    db: Session = Depends(get_db),
) -> DBLebensmittel: # Rückgabetyp angepasst
    # Ruft die korrigierte CRUD-Funktion auf
    return create_lebensmittel(db=db, lebensmittel_in=lebensmittel_in)


@router.get(
    "/",
    response_model=List[LebensmittelRead],
    status_code=status.HTTP_200_OK,
)
def read_items(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
) -> List[DBLebensmittel]: # Rückgabetyp angepasst
    # Ruft die umbenannte/korrigierte CRUD-Funktion auf
    return get_lebensmittel_list(db=db, skip=skip, limit=limit)


@router.get(
    "/{lebensmittel_id}",
    response_model=LebensmittelRead,
    status_code=status.HTTP_200_OK,
)
def read_item(
    lebensmittel_id: int,
    db: Session = Depends(get_db),
) -> DBLebensmittel: # Rückgabetyp angepasst
    # Ruft die korrigierte CRUD-Funktion auf (die jetzt nach ID sucht und 404 wirft)
    return get_lebensmittel(db=db, item_id=lebensmittel_id)


@router.patch( # Changed from PUT to PATCH for partial updates
    "/{lebensmittel_id}",
    response_model=LebensmittelRead,
    status_code=status.HTTP_200_OK,
)
def update_item( # This function now handles PATCH requests for partial updates
    lebensmittel_id: int,
    lebensmittel_in: LebensmittelUpdate, # Korrektes Schema für Update (partial)
    db: Session = Depends(get_db),
) -> DBLebensmittel: # Rückgabetyp angepasst
    # Holt das Item zuerst. get_lebensmittel wirft HTTPException (404), falls nicht gefunden.
    db_item = get_lebensmittel(db=db, item_id=lebensmittel_id)
    # Übergibt db_item und lebensmittel_in an die korrigierte CRUD-Funktion
    return update_lebensmittel(db=db, db_item=db_item, lebensmittel_in=lebensmittel_in)


@router.delete(
    "/{lebensmittel_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_item(
    lebensmittel_id: int,
    db: Session = Depends(get_db),
) -> None: # Rückgabetyp angepasst
    # Holt das Item zuerst. get_lebensmittel wirft HTTPException (404), falls nicht gefunden.
    db_item = get_lebensmittel(db=db, item_id=lebensmittel_id)
    # Übergibt db_item an die korrigierte CRUD-Funktion
    delete_lebensmittel(db=db, db_item=db_item)
    # Kein return-Body für 204