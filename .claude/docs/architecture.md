# Architecture

Full-stack expense tracker. Two independent, deployable units share a single PostgreSQL database and communicate over HTTPS/JSON.

```
┌─────────────────┐     HTTP/JSON      ┌──────────────────┐     JDBC      ┌──────────────┐
│  Next.js 15     │  ───────────────►  │  Spring Boot 3.4 │  ───────────► │ PostgreSQL 16│
│  (Server Comp.) │  Bearer JWT        │  (REST + JWT)    │  Liquibase    │              │
└─────────────────┘                    └──────────────────┘               └──────────────┘
```

## High-level Principles

- **Separation of concerns**: backend owns business rules + persistence; frontend owns presentation + UX.
- **Stateless backend**: no HTTP session — every request carries its own JWT.
- **Single source of truth**: PostgreSQL. Schema migrated only via Liquibase.
- **Security by default**: PII encrypted at rest, passwords BCrypt-hashed, JWT short-lived, refresh in HttpOnly cookie, rate limiting on every endpoint.
- **Document-as-you-code**: JavaDoc + SpringDoc on every backend public element, TSDoc on every frontend public slice export.

---

## Backend — Layered + CQRS

Root package: `com.company.expensetracker`.

```
                    ┌────────────────────┐
HTTP Request  ───►  │  controller/       │  Orchestration only. Returns ResponseEntity.
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │  service/          │  @Transactional. Business logic.
                    │   ┌──────────────┐ │  Split per resource:
                    │   │ *Command...  │ │    *CommandService  (writes, mutating)
                    │   │ *Query...    │ │    *QueryService    (reads, @Transactional(readOnly = true))
                    │   │ *Mapper      │ │    *Mapper          (MapStruct, no manual mapping)
                    │   └──────────────┘ │
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │  repository/       │  Spring Data JPA. Custom JPQL when needed.
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │  domain/           │  JPA entities + value objects. @Version on every entity.
                    └────────────────────┘

Cross-cutting:
  config/      SecurityConfig, CorsConfig, RateLimitConfig, OpenApiConfig
  security/    JwtTokenProvider, JwtAuthenticationFilter, RateLimitFilter, UserPrincipal, CustomUserDetailsService
  crypto/      AesGcmStringConverter, AesKeyHolder, EmailHasher
  audit/       SpringSecurityAuditorAware
  event/       Domain events (UserRegistered, UserLoggedIn, UserLoginFailed, PasswordChanged)
  exception/   GlobalExceptionHandler (RFC 7807), ApiAuthenticationEntryPoint, ApiAccessDeniedHandler
  dto/         Java records — request/response/patch/summary (per resource)
```

### Patterns

| Pattern | Where | Why |
|---|---|---|
| **CQRS** | `*CommandService` + `*QueryService` | Splits write transactions (slow, mutating) from read transactions (fast, read-only). Easier to reason about, easier to optimize reads independently. |
| **Spring Application Events** | `event/` + `@TransactionalEventListener(AFTER_COMMIT)` / `@EventListener @Async` | Decouples modules. E.g. `Auth` never imports `UserRepository` — it publishes `UserLoggedInEvent`, and `UserLoginActivityListener` updates user state. |
| **MapStruct** | `*Mapper` interfaces | Compile-time codegen, zero runtime cost, no manual DTO↔entity boilerplate. |
| **RFC 7807 Problem Details** | `GlobalExceptionHandler` | Standardized error format — every error response is `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`. |
| **Optimistic locking** | `@Version Long version` on every entity | Detects lost updates under concurrent writes — throws `OptimisticLockException` → 409. |
| **Auditing** | `BaseEntity` + `SpringSecurityAuditorAware` | Auto-fills `created_at`, `updated_at`, `created_by`, `updated_by` from the current `Authentication`. |
| **Field-level encryption** | `AesGcmStringConverter` + `@Convert` | PII (`email`, `firstName`, `lastName`) encrypted at the JPA boundary — DB never sees plaintext. |
| **Deterministic lookup hash** | `EmailHasher` (SHA-256) + `email_hash` column | Encrypted columns cannot be queried — `email_hash` provides a deterministic, indexable lookup key. |

### Modules

| Module | Responsibility | Public API surface |
|---|---|---|
| **auth** | Authentication, JWT lifecycle, token revocation | `POST /auth/{login,refresh,logout}` |
| **user** | Registration, profile, password change | `POST /users/register`, `GET /users/me`, `POST /users/me/password` |
| **category** | User-owned expense/income categories (CRUD) | `/categories` |
| **transaction** | Financial transactions (CRUD + month/summary queries) | `/transactions` |
| **security** | JWT filter, rate limiter filter, UserDetails | Bean wiring only |
| **crypto** | AES-256-GCM converter + email hasher | JPA `@Convert` integration |
| **audit** | Domain event sink (`AuditEvent` table) | Internal — listens to events |

### Inter-module rules (enforced by review)

- `auth` MUST NOT import `repository.UserRepository` or any `service.user.*` class.
- Cross-module work happens via Spring events or the `UserDetailsService` SPI.
- `@PreAuthorize` lives on services, never on controllers.

---

## Frontend — Feature-Sliced Design (FSD)

Next.js 15 (App Router) + React 19 + TypeScript. Strict Server-Components-by-default; Client Components only for interactivity.

### Layer hierarchy — imports flow top → bottom ONLY

```
┌────────────┐
│   app/     │  Next.js routes — thin pages that delegate to views
└─────┬──────┘
      ▼
┌────────────┐
│   views/   │  Page-level Server Component compositions (parallel data fetching)
└─────┬──────┘
      ▼
┌────────────┐
│  widgets/  │  Composite, page-independent UI blocks (SidebarNav, TransactionsKpi)
└─────┬──────┘
      ▼
┌────────────┐
│ features/  │  User-facing actions (forms + Server Actions): login, create-transaction
└─────┬──────┘
      ▼
┌────────────┐
│ entities/  │  Domain models — types + small presentational components (CategoryCard, Amount)
└─────┬──────┘
      ▼
┌────────────┐
│  shared/   │  Framework-agnostic: API client, DTOs, design system, utilities
└────────────┘
```

`features → entities` is allowed; `entities → features` is **forbidden**.

### Slice internal layout

Every slice (`features/*`, `entities/*`, `widgets/*`, `views/*`) follows:

```
<slice>/
  api/         Server Actions or fetch helpers ('use server')
  model/       types, zod schemas, store
  ui/          React components
  index.ts     public barrel — the ONLY allowed import surface
```

Other slices import from `<slice>` (the barrel), never from `<slice>/ui/...`.

### Data-flow patterns

| Pattern | Where | Why |
|---|---|---|
| **Server Components fetch directly** | `views/*` | No `useEffect` for data. Pages fetch on the server, in parallel via `Promise.all`. |
| **Server Actions for mutations** | `features/*/api/*.action.ts` (`'use server'`) | No client-side API routes for writes. Built-in CSRF protection, cookies on the server. |
| **`backendFetch` is server-only** | `shared/api/http.ts` | Browser never talks to the backend directly — all calls proxied through Next.js. JWT lives in `HttpOnly` cookie. |
| **react-hook-form + zod v4** | `features/*/model/schema.ts` + `ui/*Form.tsx` | Single schema validates on both client (UX) and server (Action). Use `.issues`, not `.errors`. |
| **Route protection** | `middleware.ts` | Redirects to `/login` if `accessToken` cookie is missing. |
| **Toast notifications** | `sonner` | `toast.error` / `toast.success` from any client component. `<Toaster />` mounted in root `layout.tsx`. |

### Routes

| Group | Routes | Layout responsibility |
|---|---|---|
| `(auth)` | `/login`, `/register` | Public — no auth check |
| `(authenticated)` | `/transactions`, `/categories`, `/profile` | Layout calls `/users/me`; redirects if 401 |

---

## Security Architecture

```
┌────────────┐    ┌────────────────────┐    ┌────────────────────┐    ┌──────────────────────┐
│  Request   │ ─► │  RateLimitFilter   │ ─► │ JwtAuthFilter      │ ─► │ Spring Security      │ ─► Controller
│  (Bearer)  │    │  (Bucket4j/Caf.)   │    │ (sets Authentication)│    │ FilterChain + RBAC │
└────────────┘    └────────────────────┘    └────────────────────┘    └──────────────────────┘
```

| Concern | Mechanism |
|---|---|
| **Authentication** | JWT HS256, 15-min access token (header), 7-day refresh token (HttpOnly cookie, `Path=/api/v1/auth`, `SameSite=Strict`) |
| **Authorization** | Role-based via `Role` enum (`ROLE_USER`, `ROLE_ADMIN`) + `@PreAuthorize` on services |
| **Password storage** | BCrypt strength=12 |
| **PII at rest** | AES-256-GCM with random IV per row, stored as `Base64(IV‖ciphertext‖tag)` |
| **Email lookups** | SHA-256 hash of `lower(trim(email))` stored in indexable `email_hash` column |
| **Token revocation** | `RevokedTokenJti` table + `TokenBlacklistService` — checked on every refresh/logout |
| **Rate limiting** | Bucket4j in-memory, per-IP. `/auth/**` = 10 RPM, default = 60 RPM → 429 |
| **Brute-force defence** | `failed_login_attempts` / `locked_until` on `User`, driven by `UserLoginActivityListener` |
| **Audit trail** | `AuditEvent` JSONB rows for `UserRegistered`, `UserLoggedIn`, `UserLoginFailed` |
| **CORS** | Whitelist origins only; wildcard forbidden |
| **Errors** | RFC 7807 — no stack traces leak |

---

## Cross-cutting Decisions

- **PostgreSQL only** — `BigDecimal(19,4)` for money, `TIMESTAMP WITH TIME ZONE` for `transactions.date`, `jsonb` for audit payload.
- **Liquibase, never `ddl-auto: update`** — schema changes are reviewed XML changesets with `<rollback>` blocks.
- **Java Records for all DTOs** — immutable, concise, predictable serialization.
- **No `any` in TypeScript, no raw `Object` in Java** — strict type discipline.
- **`smoke-test.sh` (35 checks)** is the contract between backend and frontend — runs in CI before every merge.
