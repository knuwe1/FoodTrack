# backend/src/core/security.py

# Importiere datetime und NEU: timezone oder UTC
from datetime import datetime, timedelta, timezone # timezone oder UTC hinzufügen
from passlib.context import CryptContext
from jose import JWTError, jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from src.db.session import get_db
from src.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/users/login")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)


def authenticate_user(db: Session, email: str, password: str):
    from src.crud.user import get_user_by_email
    user = get_user_by_email(db, email)
    if not user or not verify_password(password, user.hashed_password):
        return None
    return user


def create_access_token(
    data: dict, expires_delta: timedelta | None = None
) -> str:
    to_encode = data.copy()
    # Verwende datetime.now(timezone.utc) statt datetime.utcnow()
    # Stelle sicher, dass du 'from datetime import timezone' importierst
    # Alternativ (ab Python 3.11): from datetime import UTC
    now = datetime.now(timezone.utc)
    expire = now + (
        expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    to_encode.update({"exp": expire})
    # Der Algorithmus sollte in den Settings definiert sein oder sicher als Konstante
    # HS256 ist ok, aber vermeide hardcoding, falls möglich
    algorithm = "HS256"
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=algorithm)


def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
):
    from src.crud.user import get_user_by_email
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        # Verwende den gleichen Algorithmus wie beim Encoding
        algorithm = "HS256"
        payload = jwt.decode(
            token, settings.SECRET_KEY, algorithms=[algorithm] # algorithms als Liste
        )
        email: str | None = payload.get("sub") # Sicherer Zugriff mit .get()
        if email is None:
            raise credentials_exception
        # Optional: Überprüfen, ob das Token abgelaufen ist (obwohl jwt.decode das tun sollte)
        # exp = payload.get("exp")
        # if exp is None or datetime.now(timezone.utc) > datetime.fromtimestamp(exp, tz=timezone.utc):
        #    raise credentials_exception

    except JWTError:
        raise credentials_exception

    user = get_user_by_email(db, email)
    if user is None:
        raise credentials_exception
    # Optional: Prüfen, ob der Benutzer aktiv ist
    # if not user.is_active:
    #     raise HTTPException(status_code=400, detail="Inactive user")
    return user