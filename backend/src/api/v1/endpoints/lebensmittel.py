# backend/src/api/v1/endpoints/lebensmittel.py

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from src.schemas.lebensmittel import (
    LebensmittelCreate,
    LebensmittelRead,
    LebensmittelUpdate,
)
from src.crud.lebensmittel import (
    create_lebensmittel,
    get_lebensmittel,
    get_lebensmittel_list,
    update_lebensmittel,
    delete_lebensmittel,
)
from src.db.session import get_db

router = APIRouter()

@router.post(
    "/",
    response_model=LebensmittelRead,
    status_code=status.HTTP_200_OK,
)
def create_item(
    lebensmittel_in: LebensmittelCreate,
    db: Session = Depends(get_db),
):
    return create_lebensmittel(db, lebensmittel_in)


@router.get(
    "/",
    response_model=List[LebensmittelRead],
    status_code=status.HTTP_200_OK,
)
def read_items(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
):
    return get_lebensmittel_list(db, skip=skip, limit=limit)


@router.get(
    "/{lebensmittel_id}",
    response_model=LebensmittelRead,
    status_code=status.HTTP_200_OK,
)
def read_item(
    lebensmittel_id: int,
    db: Session = Depends(get_db),
):
    db_item = get_lebensmittel(db, lebensmittel_id)
    if not db_item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lebensmittel nicht gefunden",
        )
    return db_item


@router.put(
    "/{lebensmittel_id}",
    response_model=LebensmittelRead,
    status_code=status.HTTP_200_OK,
)
def update_item(
    lebensmittel_id: int,
    lebensmittel_in: LebensmittelUpdate,
    db: Session = Depends(get_db),
):
    db_item = get_lebensmittel(db, lebensmittel_id)
    if not db_item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lebensmittel nicht gefunden",
        )
    return update_lebensmittel(db, db_item, lebensmittel_in)


@router.delete(
    "/{lebensmittel_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_item(
    lebensmittel_id: int,
    db: Session = Depends(get_db),
):
    db_item = get_lebensmittel(db, lebensmittel_id)
    if not db_item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lebensmittel nicht gefunden",
        )
    delete_lebensmittel(db, db_item)
    # kein return-Body für 204
