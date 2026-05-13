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
- **Documentation**: every new/modified public element MUST carry JavaDoc + SpringDoc. Run `/docs-backend` for full rules and examples. NO EXCEPTIONS.

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
- For any schema change run `/liquibase-changeset` for naming rules, rollback requirements, and existing changeset list.

## Environment
Required in `backend/.env` and `application-local.yml`:
- `APP_CRYPTO_AES_KEY` — base64-encoded 32-byte AES key (`openssl rand -base64 32`)
- `APP_JWT_SECRET` — HS256 secret (`openssl rand -base64 64`)
- `SPRING_DATASOURCE_PASSWORD` — Postgres password
