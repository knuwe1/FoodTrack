# backend/src/schemas/lebensmittel.py

from datetime import date
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict

class LebensmittelBase(BaseModel):
    name: str
    # Menge wird als Alias "quantity" exponiert und ist optional
    menge: Optional[int] = Field(None, alias="quantity", ge=0) # Menge kann null sein oder >= 0
    einheit: Optional[str] = None
    ablaufdatum: Optional[date] = None
    kategorie: Optional[str] = None
    ean_code: Optional[str] = None
    mindestmenge: Optional[int] = Field(None, ge=0)  # Mindestmenge >= 0

    # Pydantic v2 Konfiguration
    model_config = ConfigDict(
        populate_by_name=True, # Ersetzt allow_population_by_field_name
        alias_generator=lambda field_name: field_name # Standardmäßig, aber explizit für Klarheit
    )


class LebensmittelCreate(LebensmittelBase):
    # Erbt die Konfiguration von LebensmittelBase
    pass

class LebensmittelUpdate(BaseModel):
    # Alle Felder sind optional für ein Partial-Update
    name: Optional[str] = None
    menge: Optional[int] = Field(None, alias="quantity", ge=0) # Wenn angegeben, muss Menge >= 0 sein
    einheit: Optional[str] = None
    ablaufdatum: Optional[date] = None
    kategorie: Optional[str] = None
    ean_code: Optional[str] = None
    mindestmenge: Optional[int] = Field(None, ge=0)  # Mindestmenge >= 0

    # Separate Konfiguration für Update, falls benötigt
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=lambda field_name: field_name
    )

class LebensmittelRead(LebensmittelBase):
    id: int

    # Pydantic v2 Konfiguration für ORM-Modus und Aliase bei der Ausgabe
    model_config = ConfigDict(
        from_attributes=True, # Ersetzt orm_mode = True
        populate_by_name=True,
        alias_generator=lambda field_name: 'quantity' if field_name == 'menge' else field_name # Stellt sicher, dass 'quantity' ausgegeben wird
    )