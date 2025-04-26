# backend/src/config.py

from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # Hier deine Umgebungsvariablen bzw. Default-Werte
    DATABASE_URL: str
    SECRET_KEY: str
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # Pydantic v2 benötigt explizite Config-Deklaration
    model_config = SettingsConfigDict(
        env_file=Path(__file__).parent.parent / ".env",
        env_file_encoding="utf-8"
    )

# Instanz, die deine Settings aus .env und Umgebung lädt
settings = Settings()
