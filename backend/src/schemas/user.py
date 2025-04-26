# backend/src/schemas/user.py

from pydantic import BaseModel, EmailStr, ConfigDict

class UserBase(BaseModel):
    email: EmailStr

class UserCreate(UserBase):
    password: str

class UserRead(UserBase):
    id: int
    # Geändert von int zu bool für Klarheit
    is_active: bool

    # Pydantic v2 Konfiguration
    model_config = ConfigDict(
        from_attributes=True # Ersetzt orm_mode = True
    )

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

class TokenData(BaseModel):
    email: EmailStr | None = None