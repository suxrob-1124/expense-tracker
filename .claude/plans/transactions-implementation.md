# План реализации: Модуль Transactions

## Context
Центральный модуль учёта доходов/расходов. Сущность `Transaction` связывает `User` ↔ `Category` ↔ денежная операция. Реализуется по образцу уже готового модуля `Category` (Шаг 5), с добавлением проверки владения категорией при создании/обновлении транзакции (cross-module ownership: транзакция не может ссылаться на чужую категорию).

Источник требований: `.claude/prompts/transactions.md`.

---

## Phase 1 — Database & Persistence

### 1.1 Liquibase changeset
- **Файл**: `backend/src/main/resources/db/changelog/changes/20260507-004-create-transactions-table.xml`
- Подключается автоматически через `<includeAll>` в `db.changelog-master.xml` — отдельная регистрация не нужна.
- Структура (по образцу `20260506-003-create-categories-table.xml` — 4 changeset'а):
  1. **createTable `transactions`**:
     - `id UUID PK`
     - `amount DECIMAL(19,4) NOT NULL` (общий стандарт проекта — scale 4, см. CLAUDE.md «Currency»)
     - `type VARCHAR(16) NOT NULL` (значения `INCOME` / `EXPENSE` — enum в Java)
     - `description VARCHAR(255) NULL`
     - `date TIMESTAMP WITH TIME ZONE NOT NULL`
     - `category_id UUID NOT NULL`
     - `user_id UUID NOT NULL`
     - `version BIGINT NOT NULL DEFAULT 0`
     - audit-колонки: `created_at`, `updated_at` (TIMESTAMP), `created_by`, `updated_by` (VARCHAR 255)
  2. **addForeignKeyConstraint** `fk_transactions_user` → `users(id)` `ON DELETE CASCADE`
  3. **addForeignKeyConstraint** `fk_transactions_category` → `categories(id)` `ON DELETE CASCADE`
  4. **createIndex** `idx_transactions_user_id` (user_id)
  5. **createIndex** `idx_transactions_category_id` (category_id)
  6. **createIndex** `idx_transactions_date` (date)
  7. (опционально) композитный `idx_transactions_user_date` (user_id, date DESC) — ускоряет основной запрос фильтра по месяцу

### 1.2 Domain
- **`backend/.../domain/TransactionType.java`** — enum `INCOME`, `EXPENSE`.
- **`backend/.../domain/Transaction.java`** — `extends BaseEntity`:
  - `@Id @GeneratedValue UUID id`
  - `@Version Long version`
  - `BigDecimal amount` (`@Column(precision=19, scale=4, nullable=false)`)
  - `@Enumerated(EnumType.STRING) TransactionType type`
  - `String description` (nullable, length 255)
  - `Instant date` (или `OffsetDateTime` — выбрать единообразно с проектом; в `BaseEntity.createdAt = Instant`, идём с `Instant`)
  - `UUID categoryId`
  - `UUID userId`
  - protected no-arg constructor + полный конструктор + getters (паттерн `Category`).

### 1.3 Repository
- **`backend/.../repository/TransactionRepository.java extends JpaRepository<Transaction, UUID>`**:
  - `List<Transaction> findAllByUserIdAndDateBetweenOrderByDateDesc(UUID userId, Instant from, Instant to)`
  - `Optional<Transaction> findByIdAndUserId(UUID id, UUID userId)`
  - (опционально) `long countByCategoryIdAndUserId(UUID categoryId, UUID userId)` — пригодится для будущих агрегатов.

---

## Phase 2 — Backend Business Logic & API

### 2.1 DTO (Records, `dto/transaction/`)
- **`TransactionRequest`** — `(BigDecimal amount, TransactionType type, String description, Instant date, UUID categoryId)`:
  - `@NotNull @Positive` на `amount` (Decimal — `@DecimalMin(value="0.01", inclusive=true)`)
  - `@NotNull` на `type`, `date`, `categoryId`
  - `@Size(max=255)` на `description`
- **`TransactionResponse`** — `(UUID id, BigDecimal amount, TransactionType type, String description, Instant date, UUID categoryId, Instant createdAt, Instant updatedAt)`.

### 2.2 Mapper
- **`service/transaction/TransactionMapper.java`** (`@Mapper(componentModel="spring")`):
  - `TransactionResponse toResponse(Transaction)`
  - `@Mapping(target="userId", ignore=true) Transaction toEntity(TransactionRequest)`
  - `void updateEntity(@MappingTarget Transaction, TransactionRequest)` — поля устанавливаются сервисом, `userId` не перезаписывается.

### 2.3 Services
- **`service/transaction/TransactionQueryService`** (`@Service`, `@Transactional(readOnly=true)`, `@PreAuthorize("hasRole('USER')")`):
  - `List<TransactionResponse> findAllForUser(UUID userId, Integer month, Integer year)`:
    - если `month`/`year` заданы → вычислить `[from, to)` через `YearMonth.of(year, month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()` и `+1 month`;
    - иначе — за **текущий месяц UTC** (выгрузка за весь период недопустима по соображениям производительности).
  - `TransactionResponse findByIdForUser(UUID id, UUID userId)` → 404 (`ResponseStatusException(NOT_FOUND)`) если не найдено.

- **`service/transaction/TransactionCommandService`** (`@Service`, `@Transactional`, `@PreAuthorize("hasRole('USER')")`):
  - `TransactionResponse create(UUID userId, TransactionRequest req)`:
    1. **Cross-module ownership check**: `categoryRepository.findByIdAndUserId(req.categoryId(), userId)` → если `empty` → 404 (`ResponseStatusException(NOT_FOUND, "Category not found")`). Это и не раскрывает существование чужих категорий, и удовлетворяет требование prompt'а.
    2. `mapper.toEntity(req)` → `setUserId(userId)` → `repository.save`.
  - `TransactionResponse update(UUID id, UUID userId, TransactionRequest req)`:
    1. `findByIdAndUserId(id, userId)` → 404.
    2. Если `req.categoryId()` отличается от текущего — повторная проверка владения категорией.
    3. `mapper.updateEntity(tx, req)` (userId не трогаем).
  - `void delete(UUID id, UUID userId)`: `findByIdAndUserId` → 404 → `repository.delete`.

- **Изоляция модулей**: `TransactionCommandService` импортирует только `CategoryRepository` (read-only ownership-проверка). Это допустимо — модуль Transaction логически зависит от Category, в отличие от запрещённой связки Auth ↔ User. Проверка через repository, не через `CategoryQueryService`, чтобы не тянуть DTO/маппинг в hot path и избежать циклических зависимостей сервисов.

### 2.4 Controller
- **`controller/transaction/TransactionController.java`** (`@RestController @RequestMapping("/api/v1/transactions")`):
  - `POST /` → `create` → `201 Created` + `Location` через `UriComponentsBuilder`.
  - `GET /?month=&year=` → `List<TransactionResponse>`.
  - `GET /{id}` → `TransactionResponse`.
  - `PATCH /{id}` → `TransactionResponse` (prompt требует `PATCH`; принимаем тот же `TransactionRequest` — фактически full-update; полноценный partial-patch можно добавить позже).
  - `DELETE /{id}` → `204 No Content`.
  - User: `@AuthenticationPrincipal UserPrincipal principal` → `principal.userId()`.

### 2.5 Security
- Endpoint защищён по умолчанию — он не входит в whitelist `SecurityConfig` (`/api/v1/auth/**`, `/api/v1/users/register`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`). Дополнительной конфигурации не требуется.

### 2.6 Ошибки
- `RFC 7807 ProblemDetail` уже централизованно обрабатывается в `GlobalExceptionHandler` — отдельные хэндлеры не нужны.

---

## Phase 3 — Frontend Infrastructure

### 3.1 Shared API
- **`frontend/src/shared/api/dto.ts`** — добавить:
  ```ts
  export type TransactionType = 'INCOME' | 'EXPENSE';
  export interface TransactionRequest {
    amount: string;       // BigDecimal сериализуется JSON-числом; на фронте удобнее string
    type: TransactionType;
    description?: string | null;
    date: string;         // ISO instant
    categoryId: string;   // UUID
  }
  export interface TransactionResponse {
    id: string;
    amount: string;
    type: TransactionType;
    description: string | null;
    date: string;
    categoryId: string;
    createdAt: string;
    updatedAt: string;
  }
  ```
  Также добавить `CategoryResponse` если он ещё не вынесен в DTO (по отчёту Explore — отсутствует).

- **`frontend/src/shared/api/endpoints.ts`** — расширить:
  ```ts
  transactions: {
    base: '/api/v1/transactions',
    byId: (id: string) => `/api/v1/transactions/${id}`,
    list: (month?: number, year?: number) =>
      `/api/v1/transactions${month && year ? `?month=${month}&year=${year}` : ''}`,
  }
  ```

### 3.2 Entity layer
- **`frontend/src/entities/transaction/`**:
  - `model/types.ts` — реэкспорт `TransactionResponse`, `TransactionType`.
  - `ui/Amount.tsx` — простой презентационный компонент `<Amount value type />` (форматирование `Intl.NumberFormat`, цвет по `type`: зелёный INCOME / красный EXPENSE).
  - `ui/TransactionItem.tsx` — строка списка (опционально, может жить в `views`).
  - `index.ts` — barrel-export.

---

## Phase 4 — Frontend Features & UI

### 4.1 Server Actions
- **`frontend/src/features/transaction-form/api/transaction.action.ts`** (`'use server'`) — по образцу `loginAction`:
  - `createTransactionAction(raw)`:
    1. `transactionSchema.safeParse(raw)` → возврат `issues` при провале.
    2. `backendFetch(API.transactions.base, { method: 'POST', body: JSON.stringify(data) })`.
    3. `parseProblemDetail` → user-friendly сообщение через `formatProblemMessage`.
    4. `revalidatePath('/transactions')`.
  - `updateTransactionAction(id, raw)` — `PATCH`, `revalidatePath`.
  - `deleteTransactionAction(id)` — `DELETE`, `revalidatePath`.
  - Все экшены требуют `accessToken` cookie — `backendFetch` форвардит автоматически (требуется проверить, что в `http.ts` accessToken пробрасывается; если нет — расширить `BackendFetchOptions`).

### 4.2 Zod schemas
- **`frontend/src/features/transaction-form/model/schema.ts`**:
  ```ts
  export const transactionSchema = z.object({
    amount: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Invalid amount').refine(v => Number(v) > 0),
    type: z.enum(['INCOME', 'EXPENSE']),
    description: z.string().max(255).optional().nullable(),
    date: z.string().datetime(),
    categoryId: z.string().uuid(),
  });
  ```
  Используется и на форме (RHF resolver), и в Server Action как defence-in-depth.

### 4.3 Transaction Form
- **`frontend/src/features/transaction-form/ui/TransactionForm.tsx`** (`'use client'`):
  - RHF + `zodResolver(transactionSchema)`.
  - Поля: amount (Input), type (Select INCOME/EXPENSE), description (Textarea), date (Input type="datetime-local"), categoryId (Select из `CategoryResponse[]`, передаётся пропсом).
  - Сабмит → `createTransactionAction` или `updateTransactionAction` (если есть `initialValue`).
  - Ошибки RFC 7807 → `toast.error`.

### 4.4 Views
- **`frontend/src/views/transactions/`**:
  - `ui/TransactionsView.tsx` — Server Component: получает `month`/`year` из `searchParams`, тянет список через `backendFetch(API.transactions.list(...))`, рендерит шапку с переключателем месяца + список + кнопку «New transaction» (открывает форму, например в Dialog).
  - `ui/MonthSwitcher.tsx` — `'use client'`, навигация через `router.push('?month=…&year=…')`.
- **`frontend/src/app/transactions/page.tsx`** — экспортирует `<TransactionsView searchParams={...}/>`.
- **`middleware.ts`** — добавить `/transactions` в `PROTECTED` массив.
- **(опционально)** на `/dashboard` — линк/виджет с последними N транзакциями.

---

## Phase 5 — Verification (Definition of Done)

### 5.1 Backend
- [ ] `./gradlew build` (с `JAVA_HOME=/opt/homebrew/opt/openjdk@21`) проходит без ошибок.
- [ ] `./gradlew test` зелёный (Testcontainers поднимается).
- [ ] `docker compose up -d postgres` + `bootRun` — приложение стартует, Liquibase применяет changeset `20260507-004` (проверить `databasechangelog`).
- [ ] В БД: таблица `transactions` создана со всеми FK/индексами (`\d transactions` в psql).

### 5.2 API smoke (расширить `scripts/smoke-test.sh`)
- [ ] Регистрация + логин — есть `accessToken`.
- [ ] `POST /api/v1/categories` → создать категорию → `categoryId`.
- [ ] `POST /api/v1/transactions` с этим `categoryId` → 201 + `Location`.
- [ ] `GET /api/v1/transactions?month=5&year=2026` → массив с одной записью.
- [ ] `GET /api/v1/transactions/{id}` → 200, корректный body.
- [ ] `PATCH /api/v1/transactions/{id}` → 200, поля обновлены.
- [ ] `DELETE /api/v1/transactions/{id}` → 204, последующий GET → 404.

### 5.3 Security & Ownership
- [ ] **Cross-user**: пользователь A пытается `GET /transactions/{id}` транзакции пользователя B → 404 (не 403, чтобы не утечь существование).
- [ ] **Cross-category**: пользователь A создаёт транзакцию с `categoryId` пользователя B → 404 «Category not found».
- [ ] **Update**: A пытается `PATCH` своей транзакции, подставляя чужой `categoryId` → 404.
- [ ] **Delete**: A пытается удалить транзакцию B → 404; запись B остаётся в БД.
- [ ] Без `accessToken` — все эндпоинты `/api/v1/transactions/**` → 401 RFC 7807.
- [ ] Невалидный body (`amount=0`, `type` отсутствует) → 400 RFC 7807 с `detail` по полям.

### 5.4 Frontend
- [ ] `npm run lint && npm run typecheck` — чисто.
- [ ] `npm run build` — успешный билд, маршрут `/transactions` присутствует.
- [ ] Без авторизации `/transactions` → редирект на `/login` (middleware).
- [ ] Создание транзакции через форму: запись появляется в списке, `toast.success`.
- [ ] Ошибка backend (например, fake `categoryId`) → `toast.error` с текстом из RFC 7807.
- [ ] Переключение месяца — list перезагружается (revalidate / re-fetch).

---

## Critical files (touch list)

**Backend (new)**
- `backend/src/main/resources/db/changelog/changes/20260507-004-create-transactions-table.xml`
- `backend/src/main/java/com/company/expensetracker/domain/Transaction.java`
- `backend/src/main/java/com/company/expensetracker/domain/TransactionType.java`
- `backend/src/main/java/com/company/expensetracker/repository/TransactionRepository.java`
- `backend/src/main/java/com/company/expensetracker/dto/transaction/TransactionRequest.java`
- `backend/src/main/java/com/company/expensetracker/dto/transaction/TransactionResponse.java`
- `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionMapper.java`
- `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionQueryService.java`
- `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionCommandService.java`
- `backend/src/main/java/com/company/expensetracker/controller/transaction/TransactionController.java`

**Backend (reused, not modified)**
- `domain/BaseEntity.java`, `domain/Category.java`, `repository/CategoryRepository.java`
- `security/UserPrincipal.java`, `exception/GlobalExceptionHandler.java`, `config/SecurityConfig.java`

**Frontend (new)**
- `frontend/src/entities/transaction/{model/types.ts, ui/Amount.tsx, index.ts}`
- `frontend/src/features/transaction-form/{model/schema.ts, ui/TransactionForm.tsx, api/transaction.action.ts}`
- `frontend/src/views/transactions/ui/{TransactionsView.tsx, MonthSwitcher.tsx}`
- `frontend/src/app/transactions/page.tsx`

**Frontend (modified)**
- `frontend/src/shared/api/dto.ts` — добавить `TransactionRequest/Response`, `TransactionType`, при необходимости `CategoryResponse`.
- `frontend/src/shared/api/endpoints.ts` — добавить ветку `transactions`.
- `frontend/src/middleware.ts` — добавить `/transactions` в protected.
- `scripts/smoke-test.sh` — секция transactions.
- `CLAUDE.md` — таблица «Implementation Status», добавить «Шаг 7 — Transaction модуль».

---

## Resolved decisions (зафиксировано техлидом)

1. **Scale of `amount`** → `DECIMAL(19,4)` (общий стандарт проекта по CLAUDE.md, единообразие расчётов важнее буквы prompt'а).
2. **Default фильтра `GET /transactions` без `month`/`year`** → текущий месяц UTC. Выгрузка за весь период не допускается из соображений производительности.
3. **PATCH семантика** → full-update (как в `Category`). Полноценный partial-update — техдолг на будущее.
