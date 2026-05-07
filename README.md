# Expense Tracker

Корпоративный трекер расходов.

## Стек

| Слой | Технологии |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4, Gradle (Kotlin DSL) |
| **Frontend** | Next.js 15, React 19, TypeScript, Tailwind CSS v4 |
| **Database** | PostgreSQL 16, Liquibase |
| **Security** | Spring Security, JWT (access + refresh), BCrypt, AES-256-GCM, rate limiting (Bucket4j), RBAC, audit-log, optimistic locking |
| **Frontend UI** | shadcn/ui, react-hook-form, zod v4, sonner |
| **Архитектура** | FSD (Feature-Sliced Design) на фронте, CQRS + Spring Events на бэке |

## Быстрый старт

### 1. Переменные окружения

```bash
cp backend/.env.example backend/.env
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
```

Сгенерируй секреты и вставь в оба файла:

```bash
openssl rand -base64 32   # → APP_CRYPTO_AES_KEY (и app.crypto.aes-key)
openssl rand -base64 64   # → APP_JWT_SECRET (и app.jwt.secret)
```

### 2. Запуск (локально)

```bash
# PostgreSQL
docker compose up -d postgres

# Backend (Java 21 keg-only — нужен явный JAVA_HOME)
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend
cd frontend && npm run dev        # http://localhost:3000
```

### 3. Запуск в Docker (бэк + фронт + БД)

```bash
docker compose up -d --build
docker compose logs -f backend
```

## Структура

```
backend/    — Spring Boot (com.company.expensetracker)
frontend/   — Next.js 15, FSD (src/app / views / features / entities / shared)
scripts/    — smoke-test.sh, e2e-frontend.sh
```

## Тесты и smoke-проверка

```bash
# Backend (требует Docker — Testcontainers)
cd backend && ./gradlew test

# E2E smoke (бэкенд должен быть запущен на :8080)
./scripts/smoke-test.sh

# E2E фронтенд (приложение должно быть запущено)
./scripts/e2e-frontend.sh
```

## Статус реализации

| Шаг | Статус | Что реализовано |
|---|---|---|
| Шаг 1 — БД и Домен | ✅ | `users` table (Liquibase), `User` entity, шифрование полей (AES-256-GCM), `EmailHasher` |
| Шаг 2 — Security Infrastructure | ✅ | `SecurityConfig`, JWT-фильтр, rate limiting, `GlobalExceptionHandler` (RFC 7807) |
| Шаг 3 — User модуль (CQRS) | ✅ | `UserCommandService`, `UserQueryService`, `UserController`, MapStruct, события |
| Шаг 4 — Auth модуль | ✅ | `AuthService`, `AuthController`, слушатели событий, `AuditEvent`, Liquibase `002` |
| Шаг 5 — Category модуль | ✅ | `Category` entity, CRUD API, Liquibase `003`, smoke-test 21/21 |
| Шаг 6 — Frontend (Auth) | ✅ | FSD-структура, shadcn/ui, страницы `/login` и `/register`, Server Actions, middleware-защита |
| Шаг 7 — Expense модуль | 🔜 | — |
