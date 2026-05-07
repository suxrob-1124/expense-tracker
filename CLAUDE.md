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

> **`smoke-test.sh`**: Bash script that hits every implemented endpoint end-to-end (register → login → CRUD for categories and transactions). Requires a running backend. Check count grows with each implemented module — see `scripts/smoke-test.sh` for the current list.

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
6. Start Postgres: `docker compose up -d postgres`
7. Run backend: `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'`
8. Run frontend: `cd frontend && npm run dev`

### Required environment variables (`backend/.env`)

| Variable | Description |
|---|---|
| `APP_CRYPTO_AES_KEY` | Base64-encoded 32-byte AES key. App refuses to start if missing or wrong length. |
| `APP_JWT_SECRET` | HS256 secret for JWT signing. |
| `SPRING_DATASOURCE_PASSWORD` | Postgres password. Set in `application-local.yml` for local dev; required env var for Docker/prod. |

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
- Existing changesets: `20260506-001-create-users-table.xml`, `20260506-002-create-audit-events-table.xml`, `20260506-003-create-categories-table.xml`, `20260507-004-create-transactions-table.xml`.

## 📋 Implementation Status

| Step | Status | What's implemented |
|---|---|---|
| Step 1 — DB & Domain | ✅ Done | `users` table (Liquibase), `User` entity, `BaseEntity`, `Role`, `UserRepository`, `AesGcmStringConverter`, `AesKeyHolder`, `EmailHasher`, `SpringSecurityAuditorAware` |
| Step 2 — Security Infrastructure | ✅ Done | `SecurityConfig`, `CorsConfig`, `RateLimitConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `RateLimitFilter`, `UserPrincipal`, `CustomUserDetailsService`, `GlobalExceptionHandler`, `ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler` |
| Step 3 — User module (CQRS) | ✅ Done | `UserRegisteredEvent`, `PasswordChangedEvent`, `RegisterRequest`, `UserResponse`, `ChangePasswordRequest`, `UserMapper` (MapStruct), `UserCommandService`, `UserQueryService`, `UserController` |
| Step 4 — Auth module + listeners | ✅ Done | `AuthService`, `AuthController`, `LoginRequest`, `AuthResponse`, `UserLoggedInEvent`, `UserLoginFailedEvent`, `AuthRegistrationListener`, `UserLoginActivityListener`, `AuthSessionListener`, `AuditEvent`, `AuditEventRepository`, Liquibase `002` changeset |
| Step 5 — Category module | ✅ Done | Liquibase `003` changeset (`categories` table + FK + indexes), `Category` entity, `CategoryRepository`, `CategoryRequest`, `CategoryResponse`, `CategoryMapper`, `CategoryQueryService`, `CategoryCommandService`, `CategoryController`, smoke-test 21/21 passed |
| Step 6 — Frontend Setup & Auth Pages (FSD) | ✅ Done | FSD structure (`shared/entities/features/widgets/views/app`), shadcn/ui manually under Tailwind 4, RHF + zod v4, Server Actions with HttpOnly cookie proxying, `/login` and `/register` pages, middleware protection for `/dashboard`, RFC 7807 → Toast (sonner). `npm run build` — OK, 5 routes. |
| Step 7 — Transaction module | ✅ Done | Liquibase changeset `20260507-004` (`transactions` table, FK, 4 indexes), `Transaction` entity + `TransactionType` enum, `TransactionRepository`, DTOs (Records), `TransactionMapper` (MapStruct), `TransactionQueryService` + `TransactionCommandService` (CQRS, cross-module ownership check via `CategoryRepository`), `TransactionController` (`PATCH` = partial update). Frontend: `entities/transaction` (Amount), `features/transaction-form` (RHF + Zod v4, Server Actions), `views/transactions` (Server Component + MonthSwitcher), `/transactions` route protected by middleware. `./gradlew build -x test` — OK, `npm run build` — OK, 6 routes. Smoke-test extended to 31 checks. |
| Step 8 — Dashboard | ✅ Done | Backend: `PagedResponse<T>` (dto/common), `findAllByUserIdOrderByDateDesc(Pageable)` in `TransactionRepository`, `TransactionQueryService.findLatest`, `GET /api/v1/transactions/latest`. Frontend (FSD): `shared/lib/formatDate`, `shared/ui/Skeleton`, `shared/api/dto.PagedResponse<T>`, `entities/user/UserGreeting`, `entities/transaction/TransactionRow`, `features/transactions-pagination/PaginationControls` (Zod client-validation), `widgets/sidebar-nav/SidebarNav` (aside A11y), `widgets/recent-transactions-list/RecentTransactionsList`, `views/dashboard/DashboardView`, `/dashboard` route fully implemented. `npm run build` — OK, 6 routes (ƒ /dashboard). Smoke-test extended to 35 checks. |
| Step 9 — UI Redesign + Categories + Profile | ✅ Done | Dropped `/dashboard`; `/` → redirect `/transactions`. Route group `app/(authenticated)/` with shared layout fetching `/users/me` and rendering `SidebarNav` (lucide icons + user card + logout). New routes: `/categories` (grid of CategoryCard + CategoryCreateForm with icon Select + color picker), `/profile` (read-only user data + logout). `widgets/transactions-kpi` (3 KPI cards: Доходы/Расходы/Баланс). `entities/category` (icons map, CategoryCard with window.confirm delete). `features/category-form` (Server Actions create/delete + RHF+zod form). `features/auth/logout` extracted. Deleted: dashboard view/route, recent-transactions-list widget, transactions-pagination feature, UserGreeting entity. `npm run build` — OK, 7 routes (ƒ /transactions, /categories, /profile). Backend smoke-test: 35/35 unchanged. |

## 🌿 Branch Strategy (GitHub Flow)

### Rules
- `main` — always deployable. Direct commits forbidden.
- All work happens in a branch cut from `main`.
- Branch lives only as long as needed for the feature/fix. Delete after merge.
- Every PR merges via **squash merge** for a linear `main` history.

### Naming Convention

| Type | Pattern | Example |
|---|---|---|
| New feature | `feat/<slug>` | `feat/dashboard-main-screen` |
| Bug fix | `fix/<slug>` | `fix/refresh-token-reuse` |
| Refactoring | `refactor/<slug>` | `refactor/transaction-mapper` |
| Documentation | `docs/<slug>` | `docs/api-endpoints` |
| Chore / CI | `chore/<slug>` | `chore/upgrade-spring-boot` |

### Workflow
```bash
# 1. Start work
git checkout main && git pull origin main
git checkout -b feat/<slug>

# 2. Work, commit using Conventional Commits
git commit -m "feat(dashboard): ..."

# 3. Before PR — sync with main
git fetch origin
git rebase origin/main

# 4. Push and open PR
git push -u origin feat/<slug>
gh pr create --base main --head feat/<slug> --title "<type>(<scope>): <subject>" --body "$(cat <<'EOF'
## Summary
- bullet points what changed and why

## Endpoints added / changed (if applicable)
| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/v1/... | ... |

## Test plan
- [ ] smoke-test passes: `./scripts/smoke-test.sh`
- [ ] backend compiles: `./gradlew build -x test`
- [ ] frontend builds: `cd frontend && npm run build`
- [ ] manual: describe golden path steps

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"

# 5. After squash merge — clean up
git checkout main && git pull origin main
git branch -d feat/<slug>
```

### PR Checklist (before opening)
- [ ] `./gradlew build -x test` — green
- [ ] `cd frontend && npm run build && npm run lint && npm run typecheck` — green
- [ ] `./scripts/smoke-test.sh` — all checks pass
- [ ] Liquibase changesets have `<rollback>` blocks
- [ ] No direct commits to `main`

## 💬 Commit Convention
Use Conventional Commits:

### Format
```
<type>(<scope>): <subject>

[optional body — wrap at 100 chars]

[optional footer — BREAKING CHANGE, Closes #issue]
```

### Rules
- **Subject line**: ≤ 72 characters, imperative mood, no period at the end.
- **Body**: explain *why*, not *what*. Wrap at 100 characters.
- **Breaking change**: add `!` after type/scope (`feat!:`) **and** a `BREAKING CHANGE:` footer.

### Types

| Type | When to use |
|---|---|
| `feat` | New feature or endpoint |
| `fix` | Bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | Performance improvement |
| `test` | Adding or updating tests / smoke-test |
| `docs` | Documentation only (`CLAUDE.md`, `README.md`) |
| `chore` | Dependencies, build scripts, CI config |
| `style` | Formatting, whitespace — no logic change |

### Scopes (optional, in parentheses)

Use the module or layer name: `auth`, `user`, `category`, `transactions`, `frontend`, `db`, `security`.

### Examples
```
feat(transactions): add PATCH partial-update with MapStruct IGNORE strategy

fix(auth): prevent refresh token reuse after logout

refactor(transactions): replace native <select> with Radix UI Select component

docs: update CLAUDE.md with commit conventions

chore(frontend): install @radix-ui/react-select

test(transactions): extend smoke-test to 32 checks including partial PATCH
```

## 🤖 AI Assistance Rules
- **Conciseness**: Code first, brief explanation after.
- **Refactoring**: Warn immediately on layered architecture violations (e.g. Auth importing User services directly).
- **Type safety**: No `any` in TypeScript. No raw `Object` in Java.
- **No shortcuts**: Never bypass security checks, never use `--no-verify`. Fix the root cause.
- **Event isolation**: If Auth needs User data, emit an event or use `UserDetailsService` SPI — never add a direct import.
- **Phase tracking**: Do not infer current implementation phase from this file. Check git log or ask.