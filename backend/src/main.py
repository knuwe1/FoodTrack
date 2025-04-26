# backend/src/main.py

import uvicorn
from fastapi import FastAPI
from contextlib import asynccontextmanager

from .db.session import init_db
from .api.v1.router import api_router
from .config import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Führt init_db() aus, wenn die App normal startet
    print("Lifespan Start: Initialisiere Datenbank...")
    init_db()
    print("Datenbank initialisiert.")
    yield
    print("Lifespan Ende.")

app = FastAPI(title="FoodTrack API", lifespan=lifespan)

app.include_router(api_router, prefix="/api/v1")

@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run(
        "backend.src.main:app",
        host=settings.UVICORN_HOST,
        port=settings.UVICORN_PORT,
        reload=settings.UVICORN_RELOAD,
    )