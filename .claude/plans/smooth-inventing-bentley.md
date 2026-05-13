# Design Document: Модуль User + Auth (JWT, CQRS via Spring Events)

## Context
Бэкенд expense-tracker сейчас — пустой скаффолд (Spring Boot 3.4 / Java 21). Все нужные зависимости уже в `build.gradle.kts` (Spring Security, JJWT 0.12.6, MapStruct, Liquibase, Hibernate Envers, Bucket4j, TestContainers), пакеты `domain/`, `security/`, `audit/`, `crypto/` и т.д. созданы, но пусты. Liquibase master changelog подключён, миграций ещё нет; `application-local.yml` настроен на `ddl-auto: validate`. Нужен фундамент аутентификации: регистрация и логин по JWT с сильной развязкой между Auth и User модулями через Spring Application Events (CQRS-подход: команды/запросы изолированы, межмодульные сайд-эффекты идут через события).

Этот план — архитектурный, без кода классов. Цель — согласовать структуру до реализации.

---

## 1. Data Layer

### 1.1 Liquibase changeset
**Файл**: `backend/src/main/resources/db/changelog/changes/20260506-001-create-users-table.xml`

Таблица `users`:

| Колонка | Тип | Constraints | Назначение |
|---|---|---|---|
| `id` | UUID | PK, NOT NULL | Идентификатор |
| `email_encrypted` | TEXT | NOT NULL | Email (зашифрован AES-GCM) |
| `email_hash` | VARCHAR(64) | NOT NULL, UNIQUE | SHA-256 от lower(email) для поиска и uniqueness |
| `password_hash` | VARCHAR(72) | NOT NULL | BCrypt-хэш |
| `first_name_encrypted` | TEXT | NULL | PII, шифрование AES-GCM |
| `last_name_encrypted` | TEXT | NULL | PII, шифрование AES-GCM |
| `role` | VARCHAR(32) | NOT NULL, DEFAULT 'ROLE_USER' | Одна из `ROLE_USER`, `ROLE_ACCOUNTANT`, `ROLE_ADMIN` |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT true | Soft-блокировка |
| `failed_login_attempts` | INT | NOT NULL, DEFAULT 0 | Для брутфорс-защиты |
| `locked_until` | TIMESTAMP | NULL | Временный лок |
| `last_login_at` | TIMESTAMP | NULL | Обновляется по событию |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Optimistic Locking |
| `created_at` | TIMESTAMP | NOT NULL | `@CreatedDate` |
| `updated_at` | TIMESTAMP | NOT NULL | `@LastModifiedDate` |
| `created_by` | VARCHAR(255) | NOT NULL | `@CreatedBy` (через `AuditorAware`) |
| `updated_by` | VARCHAR(255) | NOT NULL | `@LastModifiedBy` |

Индексы: `idx_users_email_hash` (UNIQUE).

> Почему `email_hash` отдельной колонкой: AES-GCM использует случайный IV → одно и то же значение шифруется по-разному, поиск/уникальность по `email_encrypted` невозможны. Решение — детерминированный SHA-256 от нормализованного email.

### 1.2 Entity-модель
- **`domain/BaseEntity`** (`@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`): поля `createdAt`, `updatedAt`, `createdBy`, `updatedBy` с аннотациями `@CreatedDate` / `@LastModifiedDate` / `@CreatedBy` / `@LastModifiedBy`.
- **`domain/User extends BaseEntity`**:
  - `@Id UUID id` (генерация — `GenerationType.UUID`).
  - `@Version Long version` — обязательно (правило CLAUDE.md).
  - `@Convert(converter = AesGcmStringConverter.class)` на `email`, `firstName`, `lastName`.
  - `emailHash` — обычная строка, заполняется в сервисе при создании/смене email.
  - `Role role` — Enum, маппится как `EnumType.STRING`.
  - Бизнес-методы: `incrementFailedAttempts()`, `resetFailedAttempts()`, `lockUntil(Instant)` — инкапсуляция логики, без сеттеров наружу.

### 1.3 Audit
- **`audit/SpringSecurityAuditorAware implements AuditorAware<String>`**: возвращает `SecurityContextHolder.getContext().getAuthentication().getName()`, fallback `"system"` для неавторизованных операций (регистрация).
- В `ExpenseTrackerApplication`: `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`.
- **`audit/AuditEvent`** — отдельная сущность для бизнес-аудита (не Hibernate Envers): пишется по событиям. На этапе User+Auth она нужна минимально: создаётся для `UserRegistered`, `UserLoggedIn`, `UserLoginFailed`. Подробное оформление этой таблицы — в отдельном changeset (`20260506-002-create-audit-events-table.xml`), но базово: `id`, `event_type`, `user_id`, `payload (JSONB)`, `occurred_at`, `ip_address`.

---

## 2. Security Infrastructure

### 2.1 Security Filter Chain
**`config/SecurityConfig`**:
- `SecurityFilterChain`:
  - `csrf().disable()` (stateless API + JWT).
  - `cors()` берёт `CorsConfigurationSource` из `CorsConfig`.
  - `sessionManagement().sessionCreationPolicy(STATELESS)`.
  - `authorizeHttpRequests`:
    - permit: `/api/v1/auth/**`, `/api/v1/users/register`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`.
    - всё остальное — `authenticated()`.
  - `exceptionHandling`:
    - `authenticationEntryPoint` → отдаёт Problem Details 401.
    - `accessDeniedHandler` → Problem Details 403.
  - `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.
- Бины: `PasswordEncoder` = `BCryptPasswordEncoder(12)`, `AuthenticationManager` (из `AuthenticationConfiguration`).

### 2.2 JWT
- **`security/JwtTokenProvider`**:
  - Параметры из `app.jwt.*` (`issuer`, `secret` (HS256, base64), `access-ttl=15m`, `refresh-ttl=7d`).
  - Методы: `generateAccessToken(UserPrincipal)`, `generateRefreshToken(UserPrincipal)`, `parseAndValidate(String)`, `extractUserId`, `extractRoles`.
  - Claims: `sub=userId`, `roles`, `type=access|refresh`, `jti` (для будущего blacklist).
- **`security/JwtAuthenticationFilter extends OncePerRequestFilter`**:
  - Достаёт `Authorization: Bearer …`, валидирует через `JwtTokenProvider`, кладёт `UsernamePasswordAuthenticationToken` в `SecurityContextHolder`.
  - При невалидном токене — пропускает дальше (фильтр-`EntryPoint` отработает 401).
  - **Не** ходит в БД — claim'ов достаточно (stateless).
- **`security/CustomUserDetailsService implements UserDetailsService`**:
  - Используется только при логине через `AuthenticationManager`. По JWT-фильтру в БД не ходим.
  - Загружает по `email_hash` (хэш считаем здесь же), сравнивает BCrypt пароль.
  - Возвращает `UserPrincipal` (record, реализует `UserDetails`): `userId`, `emailHash`, `passwordHash`, `role`, `enabled`.

### 2.3 Refresh-токен
- Возвращается в `HttpOnly; Secure; SameSite=Strict` cookie с путём `/api/v1/auth`.
- Access — в теле ответа.

### 2.4 Rate Limiting (заготовка)
- `config/RateLimitConfig` (Bucket4j): фильтр-обёртка перед `JwtAuthenticationFilter`, `/auth/**` — 10 RPM, всё остальное — 60 RPM. Полная реализация может быть отдельным шагом, но интерфейс закладываем сразу.

---

## 3. CQRS & Decoupling

### 3.1 Принцип
- **Auth модуль** (пакеты `auth/...`, использует `service/auth/`, `controller/auth/`, `dto/auth/`): отвечает за логин, refresh, выдачу токенов. **Никогда** не импортирует `UserService` / `UserRepository` напрямую.
- **User модуль** (пакеты `user/...`: `service/user/`, `controller/user/`, `repository/`, `domain/User`): владеет жизненным циклом пользователя (создание, обновление профиля, чтение).
- Связь:
  1. **Стандартный SPI Spring Security** — Auth получает `UserDetailsService` как бин, не зная конкретной реализации (`CustomUserDetailsService` живёт в User-модуле).
  2. **Spring Application Events** — все остальные межмодульные взаимодействия.

### 3.2 События
В пакете `event/` (общий, без бизнес-логики, только records):

| Событие | Издатель | Слушатель | Назначение |
|---|---|---|---|
| `UserRegisteredEvent(userId, emailHash, occurredAt)` | **User** (`UserCommandService` после `save()`) | **Auth** (`AuthRegistrationListener`) — пишет `AuditEvent`, опционально пре-генерит токены; **Notifications** в будущем — welcome email | Подтверждение успешной регистрации |
| `UserLoggedInEvent(userId, occurredAt, ipAddress)` | **Auth** (`AuthService.login` после успеха) | **User** (`UserLoginActivityListener`) — обновляет `last_login_at`, сбрасывает `failed_login_attempts` | Обновление активности без обратной зависимости Auth → User |
| `UserLoginFailedEvent(emailHash, occurredAt, reason)` | **Auth** | **User** (`UserLoginActivityListener`) — `incrementFailedAttempts()`, при пороге выставляет `locked_until` | Брутфорс-защита |
| `PasswordChangedEvent(userId, occurredAt)` | **User** | **Auth** (`AuthSessionListener`) — инвалидирует refresh-токены (через jti-blacklist в будущем) | Принудительный логаут после смены пароля |

Все события — `record` (immutable). Публикация — через `ApplicationEventPublisher`. Слушатели — `@EventListener` + `@Async` для не-критичных side-effects (`UserLoggedIn`, `UserLoginFailed`); `@TransactionalEventListener(phase = AFTER_COMMIT)` там, где важна транзакционная целостность (`UserRegistered`).

### 3.3 Регистрация: где живёт endpoint
- `POST /api/v1/users/register` — **в User-модуле** (создание пользователя — это команда домена User).
- Auth-модуль слушает `UserRegisteredEvent` и пишет аудит.
- Логин (`/api/v1/auth/login`) — в Auth-модуле, использует `AuthenticationManager`.

### 3.4 CQRS внутри User-модуля
- `service/user/UserCommandService` — `register(...)`, `changePassword(...)` (`@Transactional`).
- `service/user/UserQueryService` — `findById`, `findMe(...)` (`@Transactional(readOnly = true)`), возвращает DTO/Records, не Entity.
- Контроллер зовёт только эти два сервиса; репозиторий наружу не выходит.

---

## 4. Crypto

### 4.1 Шифрование PII (email, firstName, lastName)
- **`crypto/AesGcmStringConverter implements AttributeConverter<String, String>`** (`@Converter` без `autoApply` — применяем точечно через `@Convert`).
- Алгоритм: AES-256-GCM, IV 12 байт (random per encrypt), AuthTag 128 бит. Хранение: `Base64(IV || ciphertext || tag)`.
- Ключ: `app.crypto.aes-key` (env var, base64 32 байт). Загружается в синглтон `AesKeyHolder` (`@Component`), парсится один раз. Если ключ не задан — `BeanCreationException` на старте (fail-fast).
- Конвертер получает `AesKeyHolder` через статический ServiceLocator-обходной путь (JPA создаёт конвертеры рефлексией; либо использовать `AttributeConverter` с `applicationContextAware`-bridge — более чистый вариант: `@Configurable` или статический инжект через `BeanFactoryAware`-инициализатор в `@PostConstruct` главного бина). Конкретный механизм зафиксируем на этапе реализации, в плане — указатель.

### 4.2 Хэш для поиска
- `EmailHasher` (`@Component`): `sha256(lower(trim(email)))` → hex. Используется в `UserCommandService` (создание/смена email) и в `CustomUserDetailsService` (логин).

### 4.3 Пароли
- `BCryptPasswordEncoder(strength=12)` — бин в `SecurityConfig`. Хранится `password_hash` (BCrypt результат ~60 символов, поле 72 на запас).
- При `changePassword` старый пароль валидируется через `passwordEncoder.matches(...)`.

---

## 5. API Contract

### 5.1 Records (DTO)
Пакеты `dto/auth/` и `dto/user/`:

**Запросы:**
- `RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min=12, max=128) String password, @Size(max=100) String firstName, @Size(max=100) String lastName)`
- `LoginRequest(@Email @NotBlank String email, @NotBlank String password)`
- `ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min=12, max=128) String newPassword)`

**Ответы:**
- `AuthResponse(String accessToken, String tokenType /* "Bearer" */, long expiresInSeconds, UserResponse user)` — refresh идёт в HttpOnly cookie, не в теле.
- `UserResponse(UUID id, String email, String firstName, String lastName, String role, Instant createdAt)`
- Ошибки — `ProblemDetail` (RFC 7807) через `@RestControllerAdvice GlobalExceptionHandler` в `exception/`.

### 5.2 MapStruct
- `UserMapper` (`@Mapper(componentModel = "spring")`): `toResponse(User) → UserResponse`. Никакого ручного маппинга.

### 5.3 Endpoints

| Метод | Путь | Модуль | Auth | Назначение |
|---|---|---|---|---|
| POST | `/api/v1/users/register` | User | public | Регистрация (публикует `UserRegisteredEvent`) |
| GET  | `/api/v1/users/me` | User | authenticated | Текущий пользователь |
| POST | `/api/v1/users/me/password` | User | authenticated | Смена пароля (публикует `PasswordChangedEvent`) |
| POST | `/api/v1/auth/login` | Auth | public | Логин, выдача access+refresh |
| POST | `/api/v1/auth/refresh` | Auth | public (cookie) | Обмен refresh → новый access |
| POST | `/api/v1/auth/logout` | Auth | authenticated | Очистка cookie + (в будущем) blacklist jti |

`@PreAuthorize("hasRole('USER')")` ставится на сервисном слое (правило CLAUDE.md).

---

## 6. Step-by-step Execution

### Шаг 1 — БД и Домен
Файлы:
- `db/changelog/changes/20260506-001-create-users-table.xml`
- `domain/BaseEntity.java`, `domain/User.java`, `domain/Role.java`
- `repository/UserRepository.java` (методы: `findByEmailHash`, `existsByEmailHash`)
- `audit/SpringSecurityAuditorAware.java`
- `crypto/AesGcmStringConverter.java`, `crypto/AesKeyHolder.java`, `crypto/EmailHasher.java`
- `ExpenseTrackerApplication.java` — раскомментировать main, добавить `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`
- Конфиг `app.crypto.aes-key`, `app.jwt.*` в `application.yml` / `application-local.yml`

**Проверка**: `./gradlew build -x test` зелёный, `./gradlew bootRun --args='--spring.profiles.active=local'` стартует, Liquibase создаёт таблицу `users`, в БД появилась запись о changeset.

### Шаг 2 — Security Infrastructure
Файлы:
- `config/SecurityConfig.java`, `config/CorsConfig.java`
- `security/JwtTokenProvider.java`, `security/JwtAuthenticationFilter.java`
- `security/UserPrincipal.java`, `security/CustomUserDetailsService.java`
- `exception/GlobalExceptionHandler.java`, `exception/ApiAuthenticationEntryPoint.java`, `exception/ApiAccessDeniedHandler.java`

**Проверка**: запрос на любой защищённый endpoint без токена → 401 ProblemDetails; невалидный токен → 401; валидный (вручную сгенерированный в тесте) → пропускается.

### Шаг 3 — User модуль (CQRS)
Файлы:
- `dto/user/RegisterRequest.java`, `UserResponse.java`, `ChangePasswordRequest.java`
- `service/user/UserCommandService.java` (publish `UserRegisteredEvent`, `PasswordChangedEvent`)
- `service/user/UserQueryService.java`
- `service/user/UserMapper.java` (MapStruct)
- `controller/user/UserController.java` (`/users/register`, `/users/me`, `/users/me/password`)
- `event/UserRegisteredEvent.java`, `event/PasswordChangedEvent.java`

**Проверка**: `POST /api/v1/users/register` → 201, в БД пользователь, `email_encrypted` нечитаем, `email_hash` корректен, BCrypt-хэш в `password_hash`. `GET /api/v1/users/me` без токена → 401.

### Шаг 4 — Auth модуль + слушатели
Файлы:
- `dto/auth/LoginRequest.java`, `AuthResponse.java`
- `service/auth/AuthService.java` (login → `UserLoggedInEvent`, fail → `UserLoginFailedEvent`; refresh; logout)
- `controller/auth/AuthController.java` (`/auth/login`, `/auth/refresh`, `/auth/logout`)
- `event/UserLoggedInEvent.java`, `event/UserLoginFailedEvent.java`
- `service/auth/AuthRegistrationListener.java` (`@TransactionalEventListener` на `UserRegisteredEvent`, пишет AuditEvent)
- `service/user/UserLoginActivityListener.java` (`@EventListener` на `UserLoggedInEvent`/`UserLoginFailedEvent`, обновляет `last_login_at` / `failed_login_attempts`)
- `service/auth/AuthSessionListener.java` (`@EventListener` на `PasswordChangedEvent` — заготовка для blacklist)
- (если ещё не создана) миграция `20260506-002-create-audit-events-table.xml` + `domain/AuditEvent.java`

**Проверка end-to-end**:
1. `docker compose up -d` → PostgreSQL up.
2. `./gradlew bootRun --args='--spring.profiles.active=local'`.
3. `curl POST /api/v1/users/register` → 201.
4. `curl POST /api/v1/auth/login` → 200, access в теле, refresh в Set-Cookie.
5. `curl GET /api/v1/users/me` с `Authorization: Bearer …` → 200 с профилем.
6. `last_login_at` в БД обновлён (доказывает работу события).
7. 3 неудачных логина → `failed_login_attempts = 3` (доказывает второе событие).
8. `./gradlew test` (Testcontainers) — интеграционный тест полного флоу register → login → /me.

---

## Согласованные решения
- Регистрация: `POST /api/v1/users/register` (в User-модуле).
- Минимальная длина пароля: 12 символов.
- `AuditEvent` таблица + listener реализуются на Шаге 4 (миграция `20260506-002-create-audit-events-table.xml`, сущность `domain/AuditEvent`).
- Bucket4j rate limiting закладывается на Шаге 2: фильтр `RateLimitFilter` перед `JwtAuthenticationFilter`, лимиты `/auth/**` = 10 RPM, default = 60 RPM. Конфиг `config/RateLimitConfig` с `Bucket4j` in-memory backed (`Caffeine`-кеш по IP). Под Шаг 2 добавляются файлы:
  - `config/RateLimitConfig.java`
  - `security/RateLimitFilter.java`
  - проверка: 11-й запрос на `/auth/login` за минуту → 429 ProblemDetails.

---

## 📋 Implementation Checklist

> Легенда: `[ ]` — не начато · `[~]` — в процессе · `[x]` — выполнено и соответствует DoD.

---

### Шаг 1 — БД и Домен

#### Задачи
- [x] `db/changelog/changes/20260506-001-create-users-table.xml` — Liquibase changeset создаёт таблицу `users`
- [x] `domain/BaseEntity.java` — `@MappedSuperclass` с полями аудита (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`)
- [x] `domain/Role.java` — enum `ROLE_USER`, `ROLE_ACCOUNTANT`, `ROLE_ADMIN`
- [x] `domain/User.java` — сущность расширяет `BaseEntity`
- [x] `repository/UserRepository.java` — методы `findByEmailHash`, `existsByEmailHash`
- [x] `audit/SpringSecurityAuditorAware.java` — реализует `AuditorAware<String>`
- [x] `crypto/AesKeyHolder.java` — загружает ключ из `app.crypto.aes-key`, fail-fast при отсутствии
- [x] `crypto/AesGcmStringConverter.java` — `AttributeConverter<String, String>`, AES-256-GCM
- [x] `crypto/EmailHasher.java` — SHA-256 от `lower(trim(email))` → hex
- [x] `ExpenseTrackerApplication.java` — добавить `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`
- [x] `application.yml` / `application-local.yml` — секции `app.crypto.aes-key` и `app.jwt.*`

#### Definition of Done (Шаг 1)
- [x] **Records/Entities**: `User` — JPA-сущность (не record), все DTO на последующих шагах — Java Records.
- [x] **`@Version`**: поле `Long version` присутствует в `User`, Liquibase changeset содержит колонку `version BIGINT NOT NULL DEFAULT 0`.
- [x] **AES-GCM**: поля `email`, `firstName`, `lastName` аннотированы `@Convert(converter = AesGcmStringConverter.class)`; значения в БД нечитаемы (зашифрованы).
- [x] **`email_hash`**: в БД хранится SHA-256 от `lower(trim(email))` в hex; колонка `UNIQUE`; поиск по `email_encrypted` не используется.
- [x] **Аудит**: `createdAt`, `updatedAt`, `createdBy`, `updatedBy` автоматически заполняются через `@EnableJpaAuditing`; при регистрации `createdBy = "system"` (fallback `AuditorAware`).
- [x] **Fail-fast**: приложение не стартует, если `app.crypto.aes-key` не задан (`BeanCreationException`).
- [x] **Сборка**: `./gradlew build -x test` — зелёный.
- [x] **Миграция**: `./gradlew bootRun --args='--spring.profiles.active=local'` стартует; Liquibase создаёт таблицу `users`; в `databasechangelog` появляется запись о changeset `20260506-001`.
- [x] **`.gitkeep`**: удалены из директорий, в которые добавлены файлы (`domain/`, `repository/`, `audit/`, `crypto/`).

---

### Шаг 2 — Security Infrastructure

#### Задачи
- [x] `config/SecurityConfig.java` — `SecurityFilterChain`, `PasswordEncoder` (BCrypt-12), `AuthenticationManager`
- [x] `config/CorsConfig.java` — whitelist origin, без wildcard
- [x] `config/RateLimitConfig.java` — Bucket4j + in-memory ConcurrentHashMap по IP; лимиты `/auth/**` = 10 RPM, default = 60 RPM
- [x] `security/JwtTokenProvider.java` — генерация/валидация access- и refresh-токенов (HS256)
- [x] `security/JwtAuthenticationFilter.java` — `OncePerRequestFilter`, stateless (без похода в БД)
- [x] `security/RateLimitFilter.java` — фильтр перед `JwtAuthenticationFilter`
- [x] `security/UserPrincipal.java` — record, реализует `UserDetails`
- [x] `security/CustomUserDetailsService.java` — загрузка по `email_hash`; используется только `AuthenticationManager`
- [x] `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`, Problem Details (RFC 7807)
- [x] `exception/ApiAuthenticationEntryPoint.java` — 401 Problem Details
- [x] `exception/ApiAccessDeniedHandler.java` — 403 Problem Details

#### Definition of Done (Шаг 2)
- [x] **Stateless**: `JwtAuthenticationFilter` не обращается к БД при обработке JWT; `SessionCreationPolicy.STATELESS`.
- [x] **`UserPrincipal`**: объявлен как Java **record**, реализует `UserDetails`; содержит `userId`, `emailHash`, `passwordHash`, `role`, `enabled`.
- [x] **BCrypt**: `PasswordEncoder` = `BCryptPasswordEncoder(12)`; стоимость (strength) не ниже 12.
- [x] **JWT-claims**: access-токен содержит `sub=userId`, `roles`, `type=access`, `jti`; refresh-токен — `type=refresh`.
- [x] **Refresh cookie**: HttpOnly, Secure, SameSite=Strict, Path=/api/v1/auth. *(устанавливается в AuthController)*
- [x] **Problem Details**: запрос без токена → 401 с `Content-Type: application/problem+json`; запрос с недостаточными правами → 403.
- [x] **Rate Limit**: 11-й запрос на `/auth/login` за минуту → 429 Problem Details; `/users/register` (60 RPM лимит) пропускает 60 запросов и блокирует 61-й.
- [x] **CORS**: wildcard `*` в конфигурации отсутствует.
- [x] **Открытые маршруты**: `/api/v1/auth/**`, `/api/v1/users/register`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**` доступны без токена.

---

### Шаг 3 — User модуль (CQRS)

#### Задачи
- [x] `event/UserRegisteredEvent.java` — immutable record: `userId`, `emailHash`, `occurredAt`
- [x] `event/PasswordChangedEvent.java` — immutable record: `userId`, `occurredAt`
- [x] `dto/user/RegisterRequest.java` — record с `@Email @NotBlank String email`, `@NotBlank @Size(min=12,max=128) String password`, `@Size(max=100) String firstName`, `@Size(max=100) String lastName`
- [x] `dto/user/UserResponse.java` — record: `UUID id`, `String email`, `String firstName`, `String lastName`, `String role`, `Instant createdAt`
- [x] `dto/user/ChangePasswordRequest.java` — record: `@NotBlank String currentPassword`, `@NotBlank @Size(min=12,max=128) String newPassword`
- [x] `service/user/UserMapper.java` — MapStruct `@Mapper(componentModel = "spring")`: `toResponse(User) → UserResponse`
- [x] `service/user/UserCommandService.java` — `@Transactional`; публикует `UserRegisteredEvent`, `PasswordChangedEvent`
- [x] `service/user/UserQueryService.java` — `@Transactional(readOnly = true)`; возвращает DTO, не Entity
- [x] `controller/user/UserController.java` — `POST /api/v1/users/register`, `GET /api/v1/users/me`, `POST /api/v1/users/me/password`

#### Definition of Done (Шаг 3)
- [x] **Records**: `RegisterRequest`, `UserResponse`, `ChangePasswordRequest` — Java Records (`record`), не классы.
- [x] **MapStruct**: `UserMapper` — единственное место маппинга Entity → DTO; ручного маппинга нет.
- [x] **Изоляция модулей**: `UserCommandService` и `UserQueryService` не импортируют ничего из `service/auth/` или `controller/auth/`.
- [x] **CQRS-разделение**: `UserCommandService` содержит только `@Transactional`-мутирующие методы; `UserQueryService` — только `readOnly = true`.
- [x] **Event publishing**: `UserCommandService.register()` публикует `UserRegisteredEvent` через `ApplicationEventPublisher` после `save()`; `changePassword()` публикует `PasswordChangedEvent`.
- [x] **`@PreAuthorize`**: авторизация стоит на уровне сервисного слоя (`@PreAuthorize("hasRole('USER')")`), не контроллера.
- [x] **Validation**: `POST /api/v1/users/register` с паролем < 12 символов → 400 Problem Details; с невалидным email → 400.
- [x] **Дублирование email**: повторная регистрация с тем же email → 409 (проверка по `email_hash`).
- [x] **PII в ответе**: `UserResponse.email` возвращает расшифрованный email (через маппер); `email_encrypted` в БД нечитаем.
- [x] **`GET /api/v1/users/me` без токена → 401**.

---

### Шаг 4 — Auth модуль + слушатели

#### Задачи
- [x] `event/UserLoggedInEvent.java` — record: `userId`, `occurredAt`, `ipAddress`
- [x] `event/UserLoginFailedEvent.java` — record: `emailHash`, `occurredAt`, `reason`
- [x] `dto/auth/LoginRequest.java` — record: `@Email @NotBlank String email`, `@NotBlank String password`
- [x] `dto/auth/AuthResponse.java` — record: `String accessToken`, `String tokenType`, `long expiresInSeconds`, `UserResponse user`
- [x] `service/auth/AuthService.java` — login (публикует `UserLoggedInEvent`/`UserLoginFailedEvent`), refresh, logout
- [x] `controller/auth/AuthController.java` — `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`
- [x] `service/auth/AuthRegistrationListener.java` — `@TransactionalEventListener(phase = AFTER_COMMIT)` на `UserRegisteredEvent`; пишет `AuditEvent`
- [x] `service/user/UserLoginActivityListener.java` — `@EventListener @Async` на `UserLoggedInEvent` и `UserLoginFailedEvent`; обновляет `last_login_at`, `failed_login_attempts`, `locked_until`
- [x] `service/auth/AuthSessionListener.java` — `@EventListener` на `PasswordChangedEvent`; заготовка для jti-blacklist
- [x] `db/changelog/changes/20260506-002-create-audit-events-table.xml` — таблица `audit_events`: `id`, `event_type`, `user_id`, `payload (JSONB)`, `occurred_at`, `ip_address`
- [x] `domain/AuditEvent.java` — JPA-сущность для таблицы `audit_events`

#### Definition of Done (Шаг 4)
- [x] **Records**: `LoginRequest`, `AuthResponse` — Java Records.
- [x] **Изоляция Auth → User**: `AuthService` не импортирует `UserRepository`, `UserCommandService`, `UserQueryService` напрямую; взаимодействие только через `UserDetailsService` (SPI) и Spring Events.
- [x] **`@TransactionalEventListener`**: `AuthRegistrationListener` использует `phase = AFTER_COMMIT`; запись `AuditEvent` происходит только после успешного коммита транзакции регистрации.
- [x] **`@Async`**: `UserLoginActivityListener` помечен `@Async`; `last_login_at` и `failed_login_attempts` обновляются без блокировки основного потока.
- [x] **Брутфорс-защита**: 5 неудачных логинов → `failed_login_attempts >= 5` → выставляется `locked_until` (+15 мин); последующий логин → 423 (аккаунт заблокирован), что реализовано через `UserPrincipal.isAccountNonLocked()`.
- [x] **Refresh-токен**: возвращается в `HttpOnly; Secure; SameSite=Strict` cookie с `Path=/api/v1/auth`; в теле ответа — только access-токен.
- [x] **Logout**: `POST /auth/logout` очищает cookie (устанавливает `Max-Age=0`).
- [x] **AuditEvent**: после успешной регистрации в таблице `audit_events` появляется запись с `event_type = 'USER_REGISTERED'` и корректным `user_id`.
- [x] **End-to-end**: сценарий из плана (пп. 1–7) проходит полностью; `./gradlew test` (Testcontainers) зелёный.
- [x] **Никаких сырых `Object`**: ни в Java-коде, ни `any` в потенциальных TypeScript-интеграциях.

---

### Сквозные DoD-критерии (применимы к каждому шагу)

| Критерий | Требование |
|---|---|
| Java Records | Все DTO (Request/Response) — `record`, не `class` |
| `@Version` | Присутствует в `User` (и в дальнейшем — в любой денежной сущности) |
| AES-GCM | PII (`email`, `firstName`, `lastName`) зашифрованы в БД через `AesGcmStringConverter` |
| Event decoupling | Auth ↔ User общаются только через Spring Events + `UserDetailsService` SPI |
| MapStruct | Ручной маппинг Entity ↔ DTO запрещён |
| Problem Details | Все ошибки — RFC 7807 `ProblemDetail` (`application/problem+json`) |
| `@PreAuthorize` | Авторизация — на сервисном слое, не в контроллере |
| Liquibase only | `ddl-auto` не `create`/`update`; схема управляется только через changesets |
| Без wildcard CORS | `*` в origin-whitelist запрещён |
| Тест-база | Интеграционные тесты используют Testcontainers (PostgreSQL), не H2 и не моки БД |
