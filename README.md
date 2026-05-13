# Expense Tracker

Корпоративный трекер расходов.

## Стек

| Слой | Технологии |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4, Gradle (Kotlin DSL) |
| **Frontend** | Next.js 15, React 19, TypeScript, Tailwind CSS v4 |
| **Database** | PostgreSQL 16, Liquibase |
| **Security** | Spring Security, JWT (access + refresh), BCrypt, AES-256-GCM, rate limiting (Bucket4j), RBAC, audit-log |
| **Frontend UI** | shadcn/ui, react-hook-form, zod v4, sonner |
| **Архитектура** | FSD (Feature-Sliced Design) на фронте, CQRS + Spring Events на бэке |

## Быстрый старт

### Переменные окружения

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

### Локальный запуск

```bash
# PostgreSQL
docker compose up -d postgres

# Backend (Java 21 keg-only — нужен явный JAVA_HOME)
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend
cd frontend && npm run dev        # http://localhost:3000
```

### Запуск в Docker (postgres + backend + frontend)

```bash
docker compose up -d --build
docker compose logs -f backend    # логи бэкенда
docker compose logs -f frontend   # логи фронтенда
```

## Структура

```
backend/    — Spring Boot (com.company.expensetracker)
frontend/   — Next.js 15, FSD (src/app / views / features / entities / shared)
scripts/    — smoke-test.sh, e2e-frontend.sh
```

## Тесты

```bash
# Backend (требует Docker — Testcontainers)
cd backend && ./gradlew test

# E2E smoke (бэкенд должен быть запущен на :8080)
./scripts/smoke-test.sh

# E2E фронтенд (приложение должно быть запущено)
./scripts/e2e-frontend.sh
```
