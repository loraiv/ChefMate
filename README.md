# ChefMate

**ChefMate** is a recipe management app: an Android mobile application with a Spring Boot REST API backend. Users can register, create and browse recipes, manage a shopping list, use an AI cooking assistant, and recover their password via email.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [How to Use](#how-to-use)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Environment Variables](#environment-variables)
- [License](#license)

---

## Requirements

- **Java 21** – backend
- **Maven 3.6+** – or use the bundled `mvnw` in `chefmate-backend/`
- **PostgreSQL** – database for the backend
- **Android Studio** – for the Android app
- **JDK 17+** – for Android build

---

## Installation

### 1. Clone the repository

```bash
git clone <repository-url>
cd ChefMate
```

### 2. Backend dependencies

No extra step: Maven will download dependencies when you run the backend.

### 3. Android

- Open the `chefmate-android` folder in Android Studio.
- Let Gradle sync (dependencies will be downloaded automatically).

---

## Configuration

### Backend

1. **PostgreSQL** – create database and user (or use existing):

   ```sql
   CREATE DATABASE chefmate_db;
   CREATE USER postgres WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE chefmate_db TO postgres;
   ```

2. **Environment variables** – set at least:
   - `JWT_SECRET` – used to sign JWT tokens (see [Environment Variables](#environment-variables)).
   - `DB_PASSWORD` – PostgreSQL password (if not empty).

### Android

Set the API base URL in `chefmate-android/app/build.gradle.kts`:

- **Emulator:** `http://10.0.2.2:8090/`
- **Physical device:** Your computer’s IP, e.g. `http://192.168.1.11:8090/`

Phone and computer must be on the same Wi‑Fi network. Find the line with `buildConfigField("String", "BASE_URL", ...)` and update the URL.

---

## How to Run

### Backend

```bash
cd chefmate-backend
./mvnw spring-boot:run
```

**Windows:**

```cmd
cd chefmate-backend
mvnw.cmd spring-boot:run
```

Backend runs at **http://localhost:8090**.

### Android

1. Start the backend first.
2. In Android Studio: open `chefmate-android`, select an emulator or connected device.
3. Run the app.

---

## How to Use

1. **Register** – create an account (email, username, password).
2. **Login** – sign in to get a JWT token (stored and sent automatically by the app).
3. **Recipes** – browse, search, create, edit, delete; add images, ingredients, steps; like and comment.
4. **Shopping list** – add ingredients from recipes to a list.
5. **Profile** – change username, password, profile image; delete account.
6. **AI assistant** – ask the cooking assistant (requires `GEMINI_API_KEY` on the backend).
7. **Admin** – if your account has admin role, manage users from the admin panel.

---

## Project Structure

```
ChefMate/
├── chefmate-backend/           # Spring Boot REST API (Java 21)
│   ├── src/main/java/com/chefmate/backend/
│   │   ├── controller/        # REST endpoints (auth, recipes, admin, AI, etc.)
│   │   ├── service/            # Business logic
│   │   ├── repository/        # JPA repositories
│   │   ├── entity/            # JPA entities
│   │   ├── dto/               # Request/response DTOs
│   │   ├── config/            # Security, CORS, OpenAPI, etc.
│   │   ├── filter/            # JWT authentication filter
│   │   └── utils/
│   └── src/main/resources/
│       └── application.yaml   # App config (DB, JWT, mail, Gemini)
│
└── chefmate-android/          # Android app (Kotlin)
    └── app/src/main/
        ├── java/com/chefmate/ # UI, ViewModels, repositories, data
        └── res/               # Layouts, drawables, values
```

---

## API Documentation

After starting the backend:

- **Swagger UI:** http://localhost:8090/swagger-ui.html  
- **OpenAPI JSON:** http://localhost:8090/api-docs  

All endpoints (auth, recipes, shopping list, admin, AI, files) are documented there.

---

## Testing

### Backend

From the `chefmate-backend` directory:

```bash
./mvnw test
```

Runs unit tests for controllers and services (e.g. `AuthControllerTest`, `AuthServiceTest`, `RecipeControllerTest`, `RecipeServiceTest`).

### Android

- **Unit tests** – in Android Studio: right‑click `app/src/test` → Run Tests, or from terminal:
  ```bash
  cd chefmate-android
  ./gradlew test
  ```
- **Instrumented tests** – run on device/emulator: right‑click `app/src/androidTest` → Run Tests (e.g. `MainActivityTest`, `LoginActivityTest`).

