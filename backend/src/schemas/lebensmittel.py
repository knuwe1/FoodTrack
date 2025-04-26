from datetime import date
from typing import Optional
from pydantic import BaseModel, Field

class LebensmittelBase(BaseModel):
    name: str
    # Menge wird als Alias "quantity" exponiert
    menge: int = Field(..., alias="quantity")
    einheit: Optional[str] = None
    ablaufdatum: Optional[date] = None
    kategorie: Optional[str] = None

    class Config:
        # Damit Pydantic Eingaben über das Alias "quantity" annimmt
        allow_population_by_field_name = True

class LebensmittelCreate(LebensmittelBase):
    pass

class LebensmittelUpdate(BaseModel):
    # Alle Felder sind optional für ein Partial-Update
    name: Optional[str] = None
    menge: Optional[int] = Field(None, alias="quantity")
    einheit: Optional[str] = None
    ablaufdatum: Optional[date] = None
    kategorie: Optional[str] = None

    class Config:
        allow_population_by_field_name = True

class LebensmittelRead(LebensmittelBase):
    id: int

    class Config:
        orm_mode = True
        # Ausgaben verwenden das Alias ("quantity") statt "menge"
        by_alias = True
        allow_population_by_field_name = True
