# Expense Tracker

Корпоративный трекер расходов.

## Стек

- **Backend:** Java 21, Spring Boot 3.4+, Gradle (Kotlin DSL), Layered Architecture
- **Frontend:** Next.js 16 (App Router), TypeScript, Tailwind CSS
- **Database:** PostgreSQL
- **Migrations:** Liquibase
- **Security:** Spring Security + JWT, RBAC, audit-log, шифрование чувствительных полей, rate limiting, strict CORS, optimistic locking

## Структура

```
backend/   — Spring Boot приложение (com.company.expensetracker)
frontend/  — Next.js 16 приложение
```

## Команды (после установки зависимостей)

```bash
# backend
cd backend && ./gradlew bootRun

# frontend
cd frontend && npm run dev

# postgres
docker compose up -d
```

## Статус

Сейчас: только скаффолдинг структуры. Зависимости ещё не установлены.
