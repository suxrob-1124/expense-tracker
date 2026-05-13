# Backend CLAUDE.md
Java 21 / Spring Boot 3.4. Root package: `com.company.expensetracker`.

## Commands
```bash
# IMPORTANT: Java 21 is keg-only — always set JAVA_HOME before any Gradle command
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"

./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew build -x test      # compile + package, skip tests
./gradlew test               # requires Docker (Testcontainers)
./gradlew test --tests "com.company.expensetracker.SomeTest"
```

## Coding Standards — you MUST follow these
- **DTOs**: Java Records only — never classes for request/response.
- **Mapping**: MapStruct only — no manual mapping anywhere.
- **Validation**: `jakarta.validation` (`@NotNull`, `@Size`, `@Positive`, `@Email`).
- **Currency**: NEVER `float`/`double`. Use `BigDecimal` scale 4.
- **Concurrency**: `@Version Long version` on ALL entities.
- **Errors**: RFC 7807 Problem Details (`application/problem+json`) everywhere.
- **Documentation**: every new/modified public element MUST carry JavaDoc + SpringDoc — see *Documentation Rules* below. NO EXCEPTIONS.

## Documentation Rules — write it with the code (NOT later)

**Language**: JavaDoc and OpenAPI text (`summary`, `description`, `@Schema` strings) MUST be in **English**. Always.

### JavaDoc — required on every public element
| Layer | Class JavaDoc | Method JavaDoc |
|---|---|---|
| `controller/` | purpose, base path, auth requirement | summary, `@param`, `@return`, `@throws ResponseStatusException` with status codes |
| `service/*CommandService` | "Write-side CQRS service ...", `@Transactional`, `@PreAuthorize` | behaviour, ownership checks, `@param`, `@return`, `@throws` |
| `service/*QueryService` | "Read-side CQRS service ...", `@Transactional(readOnly = true)` | behaviour, validation rules (e.g. `0 ≤ page`, `1 ≤ size ≤ 50`), `@param`, `@return`, `@throws` |
| `service/*Mapper` | MapStruct intent + null policy | short line per `toResponse` / `toEntity` / `patchEntity` |
| `repository/` | one-line intent | required for non-trivial methods (`@Query`, JPQL aggregations, native queries, count-for-precondition methods) |
| `dto/` | record purpose | not required per accessor — covered by `@Schema` |

Reference: `controller/transaction/TransactionController.java`, `service/transaction/TransactionCommandService.java`.

### SpringDoc OpenAPI — required on controllers + DTOs
- **Controller class**: `@Tag(name = "<Resource>", description = "...")` + `@SecurityRequirement(name = "bearerAuth")` (unless endpoint is public).
- **Controller method**: `@Operation(summary, description)` + `@ApiResponses` with EVERY realistic status code (200/201/204/400/401/403/404/409) + `@Parameter(description, example)` on path/query params.
- **DTO record**: `@Schema(description = "...")` on the class.
- **DTO field**: `@Schema(description, example, requiredMode)` on EVERY component.
  - `requiredMode = Schema.RequiredMode.REQUIRED` for `@NotNull` fields, `NOT_REQUIRED` for optional/patch fields.
  - `BigDecimal` amounts: state scale (e.g. "scale 4") and give a realistic example (`"99.99"` / `"99.9900"`).
  - `Instant`: state "UTC ISO-8601" and give an ISO example (`"2026-05-13T10:00:00Z"`).
  - `UUID`: give a UUID example (`"3fa85f64-5717-4562-b3fc-2c963f66afa6"`).
  - Enums: list the allowed values inline (e.g. "INCOME or EXPENSE").

Reference: `dto/transaction/TransactionRequest.java`, `TransactionResponse.java`, `TransactionPatchRequest.java`, `TransactionSummaryResponse.java`.

### Bearer-auth setup
`OpenApiConfig` already declares the `bearerAuth` security scheme. New controllers only need `@SecurityRequirement(name = "bearerAuth")` at class level.

### Verification before commit
- `./gradlew build -x test` must pass.
- `docker compose up -d --build` + open `http://localhost:8080/swagger-ui/index.html` — confirm the new tag appears with full descriptions, examples and response codes.

## Layer Rules
- `Controller`: orchestration only, no business logic. Returns `ResponseEntity`.
- `Service`: `@Transactional` + logic. `@PreAuthorize` here, NOT in controller.
- `Auth` module: NEVER imports `UserRepository` or user services directly.
- Inter-module communication: Spring Application Events + `UserDetailsService` SPI only.
- `*CommandService` — mutating ops (`@Transactional`). `*QueryService` — reads (`readOnly = true`).
- Events: immutable `record`s in `event/`. `@TransactionalEventListener(AFTER_COMMIT)` for critical side-effects, `@EventListener @Async` for non-blocking.

## Package Map
| Package | Key classes |
|---|---|
| `config/` | `SecurityConfig`, `CorsConfig`, `RateLimitConfig` (Bucket4j) |
| `controller/auth/` | `AuthController` — login, refresh, logout |
| `controller/user/` | `UserController` — register, /me, password change |
| `controller/category/` | `CategoryController` — CRUD |
| `controller/transaction/` | `TransactionController` — CRUD + PATCH partial update |
| `service/auth/` | `AuthService`, `AuthRegistrationListener`, `AuthSessionListener`, `TokenBlacklistService`, `LoginTokens` |
| `service/user/` | `UserCommandService`, `UserQueryService`, `UserLoginActivityListener`, `UserMapper` |
| `service/category/` | `CategoryCommandService`, `CategoryQueryService`, `CategoryMapper` |
| `service/transaction/` | `TransactionCommandService`, `TransactionQueryService`, `TransactionMapper` |
| `domain/` | `User`, `Category`, `Transaction` (`TransactionType` enum), `AuditEvent`, `RevokedTokenJti`, `BaseEntity`, `Role` |
| `repository/` | `UserRepository`, `CategoryRepository`, `TransactionRepository`, `AuditEventRepository`, `RevokedTokenJtiRepository` |
| `event/` | `UserRegisteredEvent`, `UserLoggedInEvent`, `UserLoginFailedEvent`, `PasswordChangedEvent` |
| `security/` | `JwtTokenProvider`, `JwtAuthenticationFilter`, `RateLimitFilter`, `UserPrincipal` (record), `CustomUserDetailsService` |
| `crypto/` | `AesGcmStringConverter`, `AesKeyHolder`, `EmailHasher` |
| `audit/` | `SpringSecurityAuditorAware` |
| `dto/auth/` | `LoginRequest`, `AuthResponse` |
| `dto/user/` | `RegisterRequest`, `UserResponse`, `ChangePasswordRequest` |
| `dto/category/` | `CategoryRequest`, `CategoryResponse` |
| `dto/transaction/` | `TransactionRequest`, `TransactionPatchRequest`, `TransactionResponse` |
| `dto/common/` | `PagedResponse<T>` |
| `exception/` | `GlobalExceptionHandler`, `ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler` |

## Security Rules — IMPORTANT
- **PII encryption**: `email`, `firstName`, `lastName` encrypted via `AesGcmStringConverter` (AES-256-GCM, random IV, stored as `Base64(IV‖ciphertext‖tag)`).
- **Email lookup**: NEVER query by `email_encrypted` — always use `email_hash` via `EmailHasher` (SHA-256 of `lower(trim(email))`).
- **Passwords**: BCrypt strength=12 minimum. Never reduce.
- **CORS**: Whitelist origins only. `*` wildcard is forbidden.
- **JWT**: access token 15 min (HS256) + refresh in `HttpOnly; Secure; SameSite=Strict` cookie (7 days, `Path=/api/v1/auth`).
- **Token blacklist**: `RevokedTokenJti` table + `TokenBlacklistService`. Check on every refresh/logout.
- **Rate limiting**: Bucket4j in-memory per IP. `/auth/**` = 10 RPM, default = 60 RPM → 429 Problem Details.
- **Brute-force**: `failed_login_attempts` / `locked_until` on `User`. Logic in `UserLoginActivityListener`.
- **Audit log**: `UserRegistered`, `UserLoggedIn`, `UserLoginFailed` → `AuditEvent` record.
- Open endpoints: `/api/v1/auth/**`, `/api/v1/users/register`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`.

## Database (Liquibase)
- IMPORTANT: Liquibase manages schema — **never `ddl-auto: create/update`** (only `validate`).
- Changesets: `src/main/resources/db/changelog/changes/`, naming `YYYYMMDD-NNN-description.xml`.
- Always include `<rollback>` blocks.
- Existing changesets:
  - `20260506-001` — users table
  - `20260506-002` — audit_events table
  - `20260506-003` — categories table
  - `20260507-004` — transactions table
  - `20260507-005` — revoked_token_jtis table

## Environment
Required in `backend/.env` and `application-local.yml`:
- `APP_CRYPTO_AES_KEY` — base64-encoded 32-byte AES key (`openssl rand -base64 32`)
- `APP_JWT_SECRET` — HS256 secret (`openssl rand -base64 64`)
- `SPRING_DATASOURCE_PASSWORD` — Postgres password
