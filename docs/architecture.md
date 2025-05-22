# System Architecture

This document provides an overview of the system architecture for the Lebensmittel Tracker application.

## Backend

The backend is a FastAPI application responsible for handling business logic, data storage, and providing a RESTful API for clients.

Key modules and components:

*   **`main.py`**: The entry point of the FastAPI application. It initializes the app, sets up middleware, and includes API routers.
*   **`api/v1/router.py`**: Defines the API routes for version 1 of the API. It likely includes routers for Lebensmittel (food items) and Users.
*   **`models/`**: Contains SQLAlchemy models that define the database schema (e.g., `Lebensmittel`, `User`).
*   **`schemas/`**: Contains Pydantic schemas used for data validation and serialization/deserialization of API request and response bodies.
*   **`crud/`**: Contains functions for Create, Read, Update, and Delete (CRUD) operations on database entities. These functions interact with the database using SQLAlchemy.

## Database

*   **Development**: SQLite is used as the database for local development due to its simplicity and ease of setup.
*   **Production**: PostgreSQL is recommended for production environments, as indicated in the project's README. It offers more robust features and scalability.

## Android Client

An Android client application (as mentioned in the README) interacts with the backend by making requests to its RESTful API. This allows users to manage their food items, track expiry dates, and potentially receive notifications.

## System Diagram (Conceptual)

```
+-----------------+      +---------------------+      +-------------------+
| Android Client  |----->|   FastAPI Backend   |<---->|     Database      |
| (Mobile App)    |      | (RESTful API)       |      | (SQLite/PostgreSQL)|
+-----------------+      +---------------------+      +-------------------+
                         | - main.py           |
                         | - api/v1/router.py  |
                         | - models/           |
                         | - schemas/          |
                         | - crud/             |
                         +---------------------+
```

The Android client sends HTTP requests (e.g., GET, POST, PUT, DELETE) to specific API endpoints provided by the FastAPI backend. The backend processes these requests, interacts with the database (SQLite in development, PostgreSQL in production) for data persistence, and sends back HTTP responses to the client.
