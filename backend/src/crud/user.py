# backend/src/crud/user.py

from sqlalchemy.orm import Session
from fastapi import HTTPException

from src.models.user import User
from src.schemas.user import UserCreate
from src.core.security import get_password_hash


def get_user_by_email(db: Session, email: str) -> User:
    """
    Retrieves a user by their email address.

    Args:
        db: The database session.
        email: The email address of the user to retrieve.

    Returns:
        The User model instance if a user with the given email exists.

    Raises:
        HTTPException: If no user with the given email is found (status_code 404).
    """
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user


def create_user(db: Session, user_in: UserCreate) -> User:
    """
    Creates a new user in the database.

    The user's password will be hashed before storing.

    Args:
        db: The database session.
        user_in: The data for the new user, based on UserCreate schema.
                 This includes the email and plain text password.

    Returns:
        The newly created User model instance.

    Side effects:
        Commits the transaction to the database.
    """
    hashed_password = get_password_hash(user_in.password)
    db_user = User(
        email=user_in.email,
        hashed_password=hashed_password,
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user
