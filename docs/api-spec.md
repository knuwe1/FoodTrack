# API Specification

The API for the Lebensmittel Tracker backend is built using FastAPI, which automatically generates interactive API documentation.

## Accessing the API Documentation

When the backend server is running, you can access the API documentation through the following endpoints:

*   **Swagger UI**: `http://localhost:8000/docs`
    *   Provides an interactive interface to explore API endpoints, view request/response schemas, and even try out API calls directly in the browser.
*   **ReDoc**: `http://localhost:8000/redoc`
    *   Offers an alternative, more documentation-focused view of the API specification.

Replace `localhost:8000` with the actual host and port if the server is running elsewhere.

## Main API Resources

The API primarily revolves around two main resources:

### 1. Lebensmittel (Food Items)

*   **Description**: Represents food items that users want to track. Each item has properties like name, purchase date, expiry date, quantity, etc.
*   **Key Operations**:
    *   `POST /api/v1/lebensmittel/`: Create a new food item.
    *   `GET /api/v1/lebensmittel/`: Retrieve a list of food items (with pagination).
    *   `GET /api/v1/lebensmittel/{lebensmittel_id}`: Retrieve a specific food item by its ID.
    *   `PUT /api/v1/lebensmittel/{lebensmittel_id}`: Update an existing food item.
    *   `DELETE /api/v1/lebensmittel/{lebensmittel_id}`: Delete a food item.

### 2. Users

*   **Description**: Represents users of the application. This resource handles user accounts and authentication.
*   **Key Operations**:
    *   `POST /api/v1/users/`: Register a new user.
    *   `POST /api/v1/users/login`: Authenticate a user and obtain an access token.
    *   `GET /api/v1/users/me`: Get the details of the currently authenticated user.
    *   *(Potentially others like password update, user profile management, etc., depending on implementation)*

For detailed information on request parameters, response schemas, and authentication requirements, please refer to the auto-generated documentation at `/docs` or `/redoc`.
