# Expense Tracker

A full-stack personal finance management application.

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4, Gradle (Kotlin DSL) |
| **Frontend** | Next.js 15, React 19, TypeScript, Tailwind CSS v4 |
| **Database** | PostgreSQL 16, Liquibase |
| **Security** | Spring Security, JWT (access + refresh), BCrypt, AES-256-GCM, Bucket4j rate limiting, audit log |
| **Frontend UI** | shadcn/ui, react-hook-form, zod v4, sonner |
| **Architecture** | Feature-Sliced Design (frontend), CQRS + Spring Events (backend) |

## Requirements

- **Docker & Docker Compose** — for PostgreSQL (and optional containerised stack)
- **Java 21** — for local backend development (`brew install openjdk@21`)
- **Node.js 20+** — for local frontend development

## Quick Start

### 1. Environment variables

```bash
cp backend/.env.example backend/.env
```

Generate secrets and paste them into `backend/.env`:

```bash
openssl rand -base64 32   # → FIELD_ENCRYPTION_KEY
openssl rand -base64 64   # → JWT_SECRET
```

Frontend env (optional for local dev — defaults work out of the box):

```bash
cp frontend/.env.local.example frontend/.env.local
```

### 2. Database

```bash
docker compose up -d postgres
```

Liquibase migrations run automatically on backend startup. No manual steps needed.

### 3. Local dev server

```bash
# Backend — Java 21 is keg-only on macOS, JAVA_HOME must be set explicitly
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'
# → http://localhost:8080
# → http://localhost:8080/swagger-ui/index.html

# Frontend (separate terminal)
cd frontend && npm install && npm run dev
# → http://localhost:3000
```

### 4. Fully containerised (postgres + backend + frontend)

```bash
docker compose up -d --build

docker compose logs -f backend
docker compose logs -f frontend
```

If port `8080` is already taken locally:

```bash
BACKEND_PORT=8081 docker compose up -d --build
```

### 5. Smoke tests

```bash
# Backend must be running on :8080
./scripts/smoke-test.sh          # 35 API checks

./scripts/e2e-frontend.sh        # Frontend E2E (app must be running on :3000)
```

## Project Structure

```
expense-tracker/
├── backend/                    # Spring Boot (com.company.expensetracker)
│   ├── src/main/java/…/
│   │   ├── config/             # Security, CORS, rate limiting
│   │   ├── controller/         # auth/, user/, category/, transaction/
│   │   ├── service/            # *CommandService + *QueryService (CQRS)
│   │   ├── domain/             # JPA entities: User, Category, Transaction
│   │   ├── repository/         # Spring Data JPA repositories
│   │   ├── security/           # JWT filter, UserDetails, rate limit filter
│   │   ├── crypto/             # AES-256-GCM field encryption
│   │   ├── event/              # Domain events (UserRegistered, etc.)
│   │   ├── dto/                # Java Records (request/response)
│   │   └── exception/          # RFC 7807 Problem Details handler
│   └── src/main/resources/
│       └── db/changelog/       # Liquibase changesets
│
├── frontend/                   # Next.js 15 — Feature-Sliced Design
│   └── src/
│       ├── app/                # Next.js App Router
│       │   ├── (auth)/         # /login, /register (public)
│       │   └── (authenticated)/# /transactions, /categories, /profile
│       ├── views/              # Page-level Server Component compositions
│       ├── widgets/            # SidebarNav, TransactionsKpi
│       ├── features/           # auth forms, category-form, transaction-form
│       ├── entities/           # user, category, transaction types + UI
│       └── shared/             # backendFetch, API endpoints, UI kit (shadcn)
│
├── scripts/
│   ├── smoke-test.sh           # Backend API E2E (35 checks)
│   └── e2e-frontend.sh         # Frontend E2E
│
└── docker-compose.yml          # postgres + backend + frontend services
```

## API Endpoints

Base URL: `http://localhost:8080/api/v1`  
Interactive docs: `http://localhost:8080/swagger-ui/index.html`

### Authentication — `/api/v1/auth` (public)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/login` | Authenticate user, issue access + refresh tokens |
| `POST` | `/auth/refresh` | Refresh access token using HttpOnly cookie |
| `POST` | `/auth/logout` | Revoke refresh token and clear cookies |

### Users — `/api/v1/users`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/users/register` | Public | Register a new account |
| `GET` | `/users/me` | Bearer | Get current user profile |
| `POST` | `/users/me/password` | Bearer | Change password |

### Categories — `/api/v1/categories`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/categories` | Bearer | Create a category |
| `GET` | `/categories` | Bearer | List all categories (sorted by name) |
| `GET` | `/categories/{id}` | Bearer | Get a single category |
| `PUT` | `/categories/{id}` | Bearer | Replace category data |
| `DELETE` | `/categories/{id}` | Bearer | Delete a category |

### Transactions — `/api/v1/transactions`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/transactions` | Bearer | Create a transaction |
| `GET` | `/transactions/latest` | Bearer | List latest transactions (paginated) |
| `GET` | `/transactions` | Bearer | List transactions by month |
| `GET` | `/transactions/summary` | Bearer | Monthly income/expense summary |
| `GET` | `/transactions/{id}` | Bearer | Get a single transaction |
| `PATCH` | `/transactions/{id}` | Bearer | Partially update a transaction |
| `DELETE` | `/transactions/{id}` | Bearer | Delete a transaction |

## Security Notes

- **PII** (email, first/last name) is encrypted at rest with AES-256-GCM.
- **Passwords** are hashed with BCrypt (strength 12).
- **JWT**: 15-minute access token + 7-day refresh token in `HttpOnly; Secure; SameSite=Strict` cookie.
- **Rate limiting**: `/auth/**` — 10 RPM per IP; all other endpoints — 60 RPM.
- **Errors** follow [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) Problem Details (`application/problem+json`).
