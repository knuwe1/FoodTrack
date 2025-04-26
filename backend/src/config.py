# backend/src/config.py

# import os # Entfernen, falls nur für RUNNING_TESTS hinzugefügt
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    DATABASE_URL: str
    SECRET_KEY: str
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # Uvicorn Einstellungen (optional, falls vorher hinzugefügt)
    UVICORN_HOST: str = "0.0.0.0"
    UVICORN_PORT: int = 8000
    UVICORN_RELOAD: bool = True

    # RUNNING_TESTS entfernt

    model_config = SettingsConfigDict(
        env_file=Path(__file__).parent.parent / ".env",
        env_file_encoding="utf-8"
    )

# Instanz erstellen
settings = Settings()