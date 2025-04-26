# backend/src/schemas/lebensmittel.py

from datetime import date
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict

class LebensmittelBase(BaseModel):
    name: str
    # Menge wird als Alias "quantity" exponiert
    # Field(...) macht das Feld erforderlich
    menge: int = Field(..., alias="quantity")
    einheit: Optional[str] = None
    ablaufdatum: Optional[date] = None
    kategorie: Optional[str] = None

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
    menge: Optional[int] = Field(None, alias="quantity")
    einheit: Optional[str] = None
    ablaufdatum: Optional[date] = None
    kategorie: Optional[str] = None

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