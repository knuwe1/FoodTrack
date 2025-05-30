from fastapi import APIRouter
from src.api.v1.endpoints import lebensmittel, user, transaction

api_router = APIRouter()

api_router.include_router(
    lebensmittel.router,
    prefix="/lebensmittel",
    tags=["lebensmittel"],
)

api_router.include_router(
    user.router,
    prefix="/users",
    tags=["users"],
)

api_router.include_router(
    transaction.router,
    prefix="/transactions",
    tags=["transactions"],
)




