# CLAUDE.md — Project Guide & Rules

## ⚠️ Constraints
- **Rule**: Keep `.gitkeep` in empty directories until files are added; remove immediately when code is added.
- **Rule**: Java 21 is keg-only (Homebrew). Always set `JAVA_HOME` explicitly (see Commands).

## 🛠 Commands

```bash
# Infrastructure — DB only (local dev)
docker compose up -d postgres                # PostgreSQL 16 on 5432

# Infrastructure — DB + Backend (containerised)
docker compose up -d --build                # builds backend image, starts both services
docker compose logs -f backend              # follow backend logs

# E2E Smoke test (app must be running on :8080)
./scripts/smoke-test.sh                     # all checks; exits 0 = pass, 1 = fail
BASE_URL=http://localhost:8080 ./scripts/smoke-test.sh  # explicit target

# Backend — set JAVA_HOME first (keg-only install)
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew build -x test                      # compile + package, skip tests
./gradlew test                               # requires Docker (Testcontainers)
./gradlew test --tests "com.company.expensetracker.SomeTest"

# Frontend
cd frontend
npm run dev          # http://localhost:3000
npm run lint && npm run typecheck
```

> **Tip**: Add `export JAVA_HOME="/opt/homebrew/opt/openjdk@21"` to `~/.zshrc` to avoid setting it each time.

## 🚀 Getting Started (New Developer)

1. Copy environment template and fill in secrets:
   ```bash
   cp backend/.env.example backend/.env
   ```
2. Copy local Spring profile config and fill in dev secrets:
   ```bash
   cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
   ```
3. Generate a 32-byte AES key and base64-encode it:
   ```bash
   openssl rand -base64 32
   ```
4. Paste the result into `backend/.env` as `APP_CRYPTO_AES_KEY=<value>` **and** into `application-local.yml` as `app.crypto.aes-key`.
5. Generate a JWT secret and paste it into `application-local.yml` as `app.jwt.secret`:
   ```bash
   openssl rand -base64 64
   ```
6. Start Postgres: `docker compose up -d`
7. Run backend: `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'`
8. Run frontend: `cd frontend && npm run dev`

### Required environment variables (`backend/.env`)

| Variable | Description |
|---|---|
| `FIELD_ENCRYPTION_KEY` | Base64-encoded 32-byte AES key. App refuses to start if missing or wrong length. |
| `JWT_SECRET` | HS256 secret for JWT signing. |
| `DB_PASSWORD` | Postgres password. Set in `application-local.yml` for local dev; required env var for Docker/prod. |

> See `backend/.env.example` for a full template with comments.

## 🏗 Backend Architecture (Java 21 / Spring Boot 3.4)
Root package: `com.company.expensetracker`

### Coding Standards
- **DTOs**: Java **Records** for all Data Transfer Objects. Never use classes for request/response.
- **Mapping**: **MapStruct** for Entity ↔ DTO. No manual mapping anywhere.
- **Validation**: `jakarta.validation` (`@NotNull`, `@Size`, `@Positive`, `@Email`).
- **Currency**: NEVER `float`/`double`. Use `BigDecimal` with scale 4.
- **Concurrency**: `@Version Long version` on ALL entities (not just monetary). Already on `User`.
- **Audit**: `SpringSecurityAuditorAware` wired to `"auditorAware"` ref in `@EnableJpaAuditing`. Fallback `"system"` for unauthenticated ops (registration).

### Layer Rules
- `Controller`: Orchestration only, no business logic. Returns `ResponseEntity`.
- `Service`: Transaction boundaries (`@Transactional`). Logic lives here. `@PreAuthorize` here, NOT in controller.
- `Exception`: All errors use **RFC 7807 Problem Details** (`application/problem+json`).

### CQRS & Module Isolation
- **User module** owns `domain/User`, `repository/`, `service/user/`, `controller/user/`, `dto/user/`.
- **Auth module** owns `service/auth/`, `controller/auth/`, `dto/auth/`. Never imports `UserRepository` or user services directly.
- Inter-module communication: Spring Application Events only + `UserDetailsService` SPI.
- `UserCommandService` — mutating ops (`@Transactional`). `UserQueryService` — reads (`readOnly = true`).
- Events are immutable Java `record`s in `event/` package.
- `@TransactionalEventListener(phase = AFTER_COMMIT)` for critical side-effects (e.g. `UserRegisteredEvent`).
- `@EventListener @Async` for non-blocking side-effects (e.g. login activity update).

### Package Map

| Package | Purpose |
|---|---|
| `config/` | SecurityConfig, CorsConfig, RateLimitConfig (Bucket4j) |
| `controller/auth/` | AuthController — login, refresh, logout |
| `controller/user/` | UserController — register, /me, password change |
| `service/auth/` | AuthService, AuthRegistrationListener, AuthSessionListener |
| `service/user/` | UserCommandService, UserQueryService, UserLoginActivityListener, UserMapper |
| `repository/` | Spring Data JPA interfaces (UserRepository) |
| `domain/` | JPA entities with `@Version Long version` |
| `event/` | Immutable `record` events: UserRegisteredEvent, UserLoggedInEvent, UserLoginFailedEvent, PasswordChangedEvent |
| `security/` | JwtTokenProvider, JwtAuthenticationFilter, RateLimitFilter, UserPrincipal (record), CustomUserDetailsService |
| `audit/` | SpringSecurityAuditorAware, AuditEvent entity |
| `crypto/` | AesGcmStringConverter, AesKeyHolder, EmailHasher |
| `dto/auth/` | LoginRequest, AuthResponse (Records) |
| `dto/user/` | RegisterRequest, UserResponse, ChangePasswordRequest (Records) |
| `exception/` | GlobalExceptionHandler, ApiAuthenticationEntryPoint, ApiAccessDeniedHandler |

### Security Architecture
- JWT access token (15 min, HS256) + refresh token in `HttpOnly; Secure; SameSite=Strict` cookie (7 days, `Path=/api/v1/auth`).
- `JwtAuthenticationFilter` is stateless — no DB call on token validation. Claims are sufficient.
- `CustomUserDetailsService` is used only by `AuthenticationManager` during login.
- `UserPrincipal` is a Java `record` implementing `UserDetails`.
- Open endpoints: `/api/v1/auth/**`, `/api/v1/users/register`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`.
- All others require authentication.

## 🔐 Security & Crypto Rules
- **PII encryption**: `email`, `firstName`, `lastName` in `User` are encrypted with `AesGcmStringConverter` (AES-256-GCM, random IV per encrypt, stored as `Base64(IV‖ciphertext‖tag)`).
- **Email lookup**: NEVER query by `email_encrypted`. Always use `email_hash` (SHA-256 of `lower(trim(email))` in hex). `EmailHasher` is the single source of truth for hashing.
- **Key management**: `APP_CRYPTO_AES_KEY` must be a base64-encoded 32-byte value. Application fails to start (`BeanCreationException`) if absent or wrong length.
- **Passwords**: BCrypt strength=12 minimum. Never reduce strength.
- **Brute-force protection**: `failed_login_attempts` / `locked_until` on `User`. Logic lives in `UserLoginActivityListener`.
- **Rate limiting**: Bucket4j + `ConcurrentHashMap` in-memory bucket per IP. `/auth/**` = 10 RPM, default = 60 RPM. Exceeded → 429 Problem Details.
- **CORS**: Whitelist origins only. `*` wildcard is forbidden.
- **Audit log**: Every `UserRegistered`, `UserLoggedIn`, `UserLoginFailed` event writes an `AuditEvent` record.

## 🎨 Frontend Architecture (Next.js 15 / React 19 / FSD)

### Coding Standards
- **Router**: App Router **only** — no `pages/` directory.
- **Architecture**: Feature-Sliced Design (FSD). Layer import order (top→bottom only): `app → views → widgets → features → entities → shared`.
- **Path alias**: `@/*` → `src/*`
- **Tailwind**: v4 — configured via `@import "tailwindcss"` + `@theme {}` in `globals.css`. No `tailwind.config.ts`.
- **Type safety**: No `any`. Use `unknown` + type guards.
- **Forms**: `react-hook-form` + `zod` (v4 — use `.issues` not `.errors`). Resolver: `@hookform/resolvers/zod`.
- **UI Kit**: shadcn/ui installed manually in `shared/ui/` (no CLI). Imports use `@/shared/lib/cn`.
- **Notifications**: `sonner` (`toast.error`, `toast.success`). `<Toaster />` in root `layout.tsx`.

### Layer Rules
- **Data fetching**: Server Components fetch directly. No `useEffect` for data fetching.
- **Mutations**: Server Actions only (`'use server'`). No dedicated API route handlers for mutations.
- **Components**: Default to Server Components. Use `"use client"` only when required (interactivity, browser APIs).
- **Auth**: `accessToken` and `refreshToken` in HttpOnly cookies. Route protection in `middleware.ts`.
- **Server Actions → backend**: use `backendFetch` from `shared/api/http.ts` (server-only). Never call backend directly from browser.

### FSD Structure
```
src/
  app/                  # Next.js App Router — layouts, routes
  views/                # Page-level compositions (NOTE: named "views" to avoid clash with Next.js "pages/" router)
  widgets/              # Complex reusable blocks (currently empty)
  features/             # User-facing features (login-form, register-form, …)
  entities/             # Domain entities (user, …)
  shared/
    api/                # dto.ts, endpoints.ts, problem.ts, http.ts (server-only)
    config/             # env.ts
    lib/                # cn.ts
    ui/                 # shadcn primitives: button, input, label, card, form, sonner
middleware.ts           # Route protection
```

## 🗄 Database
- PostgreSQL 16, local credentials: `postgres/postgres`, DB: `expense_tracker`.
- Liquibase manages schema — **never `ddl-auto: create/update`**.
- Add changesets to `backend/src/main/resources/db/changelog/changes/`.
- Naming: `YYYYMMDD-NNN-description.xml`.
- Existing changesets: `20260506-001-create-users-table.xml`.

## 📋 Implementation Status

| Шаг | Статус | Что реализовано |
|---|---|---|
| Шаг 1 — БД и Домен | ✅ Готов | `users` table (Liquibase), `User` entity, `BaseEntity`, `Role`, `UserRepository`, `AesGcmStringConverter`, `AesKeyHolder`, `EmailHasher`, `SpringSecurityAuditorAware` |
| Шаг 2 — Security Infrastructure | ✅ Готов | `SecurityConfig`, `CorsConfig`, `RateLimitConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `RateLimitFilter`, `UserPrincipal`, `CustomUserDetailsService`, `GlobalExceptionHandler`, `ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler` |
| Шаг 3 — User модуль (CQRS) | ✅ Готов | `UserRegisteredEvent`, `PasswordChangedEvent`, `RegisterRequest`, `UserResponse`, `ChangePasswordRequest`, `UserMapper` (MapStruct), `UserCommandService`, `UserQueryService`, `UserController` |
| Шаг 4 — Auth модуль + слушатели | ✅ Готов | `AuthService`, `AuthController`, `LoginRequest`, `AuthResponse`, `UserLoggedInEvent`, `UserLoginFailedEvent`, `AuthRegistrationListener`, `UserLoginActivityListener`, `AuthSessionListener`, `AuditEvent`, `AuditEventRepository`, Liquibase `002` changeset |
| Шаг 5 — Category модуль | ✅ Готов | Liquibase `003` changeset (`categories` table + FK + индексы), `Category` entity, `CategoryRepository`, `CategoryRequest`, `CategoryResponse`, `CategoryMapper`, `CategoryQueryService`, `CategoryCommandService`, `CategoryController`, smoke-test (21/21 passed) |
| Шаг 6 — Frontend Setup & Auth Pages (FSD) | ✅ Готов | FSD-структура (`shared/entities/features/widgets/views/app`), shadcn/ui вручную под Tailwind 4, RHF + zod v4, Server Actions с HttpOnly cookie-проксированием, страницы `/login` и `/register`, middleware-защита `/dashboard`, RFC 7807 → Toast (sonner). `npm run build` — OK, 5 маршрутов. |

## 🤖 AI Assistance Rules
- **Conciseness**: Code first, brief explanation after.
- **Refactoring**: Warn immediately on layered architecture violations (e.g. Auth importing User services directly).
- **Type safety**: No `any` in TypeScript. No raw `Object` in Java.
- **No shortcuts**: Never bypass security checks, never use `--no-verify`. Fix the root cause.
- **Event isolation**: If Auth needs User data, emit an event or use `UserDetailsService` SPI — never add a direct import.
- **Phase tracking**: Do not infer current implementation phase from this file. Check git log or ask.