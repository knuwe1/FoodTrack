# backend/src/api/v1/endpoints/user.py

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from src.schemas.user import UserCreate, UserRead, Token, UserLogin
from src.crud.user import create_user, get_user_by_email
from src.core.security import authenticate_user, create_access_token, get_current_user
from src.db.session import get_db

router = APIRouter()

@router.post(
    "/",
    response_model=UserRead,
    status_code=status.HTTP_201_CREATED, # Changed to 201 for resource creation
)
def register(user_in: UserCreate, db: Session = Depends(get_db)):
    try:
        existing_user = get_user_by_email(db, user_in.email)
        # If get_user_by_email returns (doesn't raise 404), it means user exists
        if existing_user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email already registered"
            )
    except HTTPException as e:
        # Only catch the specific 404 from get_user_by_email
        if e.status_code == 404 and e.detail == "User not found":
            # This is the desired case: user does not exist, so proceed to create
            pass
        else:
            # Re-raise any other HTTPException
            raise

    # If we are here, it means either get_user_by_email raised 404 (user not found)
    # or it unexpectedly returned None (which shouldn't happen with new crud logic but good to be safe)
    # or it found a user but the logic above has a flaw.
    # Given the new crud.get_user_by_email, we expect a 404 if not found.
    return create_user(db, user_in)

@router.post(
    "/login",
    response_model=Token,
    status_code=status.HTTP_200_OK,
)
def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db)
):
    user = authenticate_user(db, form_data.username, form_data.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials"
        )
    token = create_access_token({"sub": user.email})
    return {"access_token": token, "token_type": "bearer"}

@router.post(
    "/login-json",
    response_model=Token,
    status_code=status.HTTP_200_OK,
)
def login_json(
    login_data: UserLogin,
    db: Session = Depends(get_db)
):
    """
    JSON-basierter Login-Endpunkt für mobile Apps
    """
    user = authenticate_user(db, login_data.username, login_data.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials"
        )
    token = create_access_token({"sub": user.email})
    return {"access_token": token, "token_type": "bearer"}

@router.get(
    "/me",
    response_model=UserRead,
    status_code=status.HTTP_200_OK
)
def read_current_user(current_user: UserRead = Depends(get_current_user)):
    return current_user
