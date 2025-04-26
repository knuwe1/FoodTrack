import uvicorn
from fastapi import FastAPI
from .db.session import init_db
from .api.v1.router import api_router

app = FastAPI(title="FoodTrack API")

# Datenbank initialisieren (Tabellen erstellen)
init_db()

# API-Router einbinden
app.include_router(api_router, prefix="/api/v1")

@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run("backend.src.main:app", host="0.0.0.0", port=8000, reload=True)