# План документирования модуля Transactions

## Context

Модуль Transactions полностью реализован и покрыт smoke-тестами (35/35), но **полностью лишён технической документации**: ни одного блока JavaDoc/TSDoc, ни одной аннотации SpringDoc OpenAPI (`@Tag`, `@Operation`, `@Schema`). При этом библиотека `springdoc-openapi-starter-webmvc-ui` версии 2.6.0 уже подключена в `backend/build.gradle.kts:59`, а Swagger UI отдаётся по `/swagger-ui/**`, но генерируется только из сигнатур методов — без описаний, примеров и кодов ответов.

**Цель:** обеспечить 100% документационное покрытие модуля Transactions:
- **Backend** — JavaDoc на public-методах + SpringDoc-аннотации на контроллере и DTO.
- **Frontend** — TSDoc на публичных API всех FSD-слоёв транзакций.
- **Workflow** — отдельная ветка `docs/transactions-api-doc`, conventional commits (`docs(...)`), squash merge.

После выполнения Swagger UI должен показывать структурированную документацию, а IDE — подсказки при наведении на функции и типы фронтенда.

---

## Phase 1 — Backend: JavaDoc

### 1.1 Контроллер
**Файл:** `backend/src/main/java/com/company/expensetracker/controller/transaction/TransactionController.java`

- [ ] JavaDoc уровня класса: назначение, базовый путь `/api/v1/transactions`, требование аутентификации.
- [ ] `create(...)` — параметры, возвращаемое значение, 201 Created, ошибки 400/404.
- [ ] `getLatest(page, size, ...)` — пагинация, ограничения `0 ≤ page`, `1 ≤ size ≤ 50`.
- [ ] `getAll(month, year, ...)` — фильтрация по месяцу/году, поведение при отсутствии параметров.
- [ ] `summary(month, year, ...)` — структура `TransactionSummaryResponse` (income/expense/balance).
- [ ] `getById(id, ...)` — 404 если транзакция чужая или не существует.
- [ ] `update(id, request, ...)` — partial update через `TransactionPatchRequest`, optimistic locking (`@Version`).
- [ ] `delete(id, ...)` — 204 No Content, идемпотентность.

**Стиль:** `@param`, `@return`, `@throws ResponseStatusException` — единообразно по всему модулю.

### 1.2 Command-сервис
**Файл:** `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionCommandService.java`

- [ ] JavaDoc класса: ответственность (write-side CQRS), `@Transactional`, `@PreAuthorize("hasRole('USER')")`.
- [ ] `create(UUID userId, TransactionRequest request)` — проверка владения категорией, `@throws ResponseStatusException 404`.
- [ ] `update(UUID userId, UUID id, TransactionPatchRequest patch)` — MapStruct `patchEntity` (IGNORE null), оптимистическая блокировка.
- [ ] `delete(UUID userId, UUID id)` — проверка владения перед удалением.

### 1.3 Query-сервис
**Файл:** `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionQueryService.java`

- [ ] JavaDoc класса: read-side CQRS, `@Transactional(readOnly = true)`.
- [ ] `findAllForUser(UUID userId, Integer month, Integer year)` — фильтрация по периоду.
- [ ] `findLatest(UUID userId, int page, int size)` — валидация (0 ≤ page, 1 ≤ size ≤ 50), `@throws IllegalArgumentException`.
- [ ] `findByIdForUser(UUID userId, UUID id)`.
- [ ] `summarize(UUID userId, Integer month, Integer year)` — native-query `sumByTypeForPeriod`, агрегация по `TransactionType`.

### 1.4 Маппер
**Файл:** `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionMapper.java`

- [ ] JavaDoc уровня интерфейса: MapStruct, политика NULL.
- [ ] `toResponse`, `toEntity`, `patchEntity` — короткое описание каждого.

### 1.5 Репозиторий
**Файл:** `backend/src/main/java/com/company/expensetracker/repository/TransactionRepository.java`

- [ ] JavaDoc на нетривиальные методы: `sumByTypeForPeriod` (native query, GROUP BY), `countByCategoryIdAndUserId` (используется при удалении категорий).

---

## Phase 2 — Backend: SpringDoc OpenAPI

### 2.1 Контроллер
**Файл:** `TransactionController.java`

- [ ] `@Tag(name = "Transactions", description = "Управление личными финансовыми операциями")` на класс.
- [ ] Для каждого метода: `@Operation(summary = "...", description = "...")`.
- [ ] `@ApiResponses` со списком кодов: 200/201/204/400/401/403/404/409.
- [ ] `@Parameter(description = "...", example = "...")` на path/query параметрах (`month`, `year`, `page`, `size`, `id`).
- [ ] `@SecurityRequirement(name = "bearerAuth")` (если в проекте уже определена security-схема — проверить наличие `OpenApiConfig`; если нет — пункт 2.3).

### 2.2 DTO
**Файлы:** `dto/transaction/TransactionRequest.java`, `TransactionResponse.java`, `TransactionPatchRequest.java`, `TransactionSummaryResponse.java`.

- [ ] `@Schema(description = "...")` уровня класса.
- [ ] `@Schema(description = "...", example = "...", requiredMode = REQUIRED|NOT_REQUIRED)` на каждом поле record.
- [ ] Для `amount`: указать формат BigDecimal (scale 4), пример `"100.00"`.
- [ ] Для `type`: enum-описание `INCOME`/`EXPENSE`.
- [ ] Для `date`: формат ISO-8601 (Instant), пример `"2026-05-13T10:00:00Z"`.
- [ ] Для `categoryId`: UUID, пример.

### 2.3 Глобальная конфигурация (если отсутствует)
**Файл (создать при необходимости):** `backend/src/main/java/com/company/expensetracker/config/OpenApiConfig.java`

- [ ] Проверить через `git grep -l "OpenAPI"` — есть ли уже бин `OpenAPI`.
- [ ] Если нет — добавить минимальную конфигурацию: `info` (title, version, description) + bearer-auth security scheme.
- [ ] Убедиться, что `/swagger-ui/**` и `/v3/api-docs/**` открыты в `SecurityConfig` (если уже открыто — пропустить).

---

## Phase 3 — Frontend: TSDoc

### 3.1 Shared DTOs
**Файл:** `frontend/src/shared/api/dto.ts`

- [ ] TSDoc для `TransactionRequest`, `TransactionResponse`, `TransactionSummaryResponse`, `TransactionType`.
- [ ] Указать соответствие Java records, формат `amount` (string для точности), формат `date` (ISO-8601).
- [ ] `@see` ссылки на backend-классы (опционально).

### 3.2 API endpoints
**Файл:** `frontend/src/shared/api/endpoints.ts`

- [ ] TSDoc на объекте `API.transactions`: `base`, `byId`, `list`, `latest`, `summary` — назначение и query-параметры.

### 3.3 Entity layer
**Файл:** `frontend/src/entities/transaction/index.ts` + `model/types.ts`, `ui/Amount.tsx`, `ui/TransactionRow.tsx`.

- [ ] TSDoc на ре-экспортах типов в `model/types.ts`.
- [ ] TSDoc на компонентах `Amount` (формат RUB, цвет по типу) и `TransactionRow` (props, использование).
- [ ] Документация в публичном API (`index.ts`).

### 3.4 Feature layer (transaction-form)
**Файл:** `frontend/src/features/transaction-form/`

- [ ] `api/transaction.action.ts` — TSDoc на `createTransactionAction`, `updateTransactionAction`, `deleteTransactionAction`: входы/выходы, формат union-result `{ ok: true } | { ok: false, ... }`, ревалидация.
- [ ] `model/schema.ts` — TSDoc на `transactionSchema` с описанием regex для amount.
- [ ] `ui/TransactionForm.tsx` — TSDoc props (режим create/edit, defaultValues).
- [ ] `index.ts` — публичный API.

### 3.5 View / Widget layers
- [ ] `frontend/src/views/transactions/ui/TransactionsView.tsx` — TSDoc на Server Component (параллельный fetch 3 эндпоинтов).
- [ ] `frontend/src/views/transactions/ui/MonthSwitcher.tsx`, `NewTransactionButton.tsx` — короткий TSDoc.
- [ ] `frontend/src/widgets/transactions-kpi/ui/TransactionsKpi.tsx` — TSDoc props.
- [ ] `index.ts` в каждом слое — экспорты прокомментированы.

---

## Phase 4 — Git Workflow

- [ ] Создать ветку: `git checkout -b docs/transactions-api-doc` (от `main`).
- [ ] Коммиты по conventional commits (атомарные, по подзадачам):
  - `docs(transactions): add JavaDoc to controller and services`
  - `docs(transactions): add SpringDoc annotations to controller and DTOs`
  - `docs(transactions): add TSDoc to shared DTOs and API endpoints`
  - `docs(transactions): add TSDoc to entity/feature/view layers`
- [ ] Сообщения ≤ 72 символа, императив, без точки в конце.
- [ ] PR через **squash merge**.

---

## Phase 5 — Verification

### 5.1 Backend
- [ ] `./gradlew build -x test` — сборка проходит (документация не должна ломать компиляцию).
- [ ] `./gradlew javadoc` (если задача доступна) — без warnings о missing `@param`/`@return` в модуле transactions.
- [ ] Запустить backend: `docker compose up -d --build`.
- [ ] Открыть `http://localhost:8080/swagger-ui/index.html`:
  - [ ] Виден `Tag "Transactions"`.
  - [ ] Все 7 эндпоинтов с summary, описанием, кодами ответов.
  - [ ] DTO раскрываются с описанием полей и примерами.
  - [ ] `Try it out` работает (после авторизации).
- [ ] `curl http://localhost:8080/v3/api-docs | jq '.paths."/api/v1/transactions"'` — JSON содержит описания.

### 5.2 Frontend
- [ ] `npm run typecheck` — без ошибок.
- [ ] `npm run lint` — без warnings.
- [ ] `npm run build` — успешная сборка.
- [ ] В VS Code/WebStorm: hover на `createTransactionAction`, `TransactionResponse`, `<Amount>` показывает TSDoc.

### 5.3 E2E
- [ ] `./scripts/smoke-test.sh` — 35/35 проходят (документация не должна затронуть behaviour).

---

## Критические файлы (для исполнителя)

**Backend:**
- `backend/src/main/java/com/company/expensetracker/controller/transaction/TransactionController.java`
- `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionCommandService.java`
- `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionQueryService.java`
- `backend/src/main/java/com/company/expensetracker/service/transaction/TransactionMapper.java`
- `backend/src/main/java/com/company/expensetracker/repository/TransactionRepository.java`
- `backend/src/main/java/com/company/expensetracker/dto/transaction/*.java` (4 файла)
- `backend/src/main/java/com/company/expensetracker/config/OpenApiConfig.java` (создать при отсутствии)

**Frontend:**
- `frontend/src/shared/api/dto.ts`
- `frontend/src/shared/api/endpoints.ts`
- `frontend/src/entities/transaction/index.ts`, `model/types.ts`, `ui/Amount.tsx`, `ui/TransactionRow.tsx`
- `frontend/src/features/transaction-form/index.ts`, `api/transaction.action.ts`, `model/schema.ts`, `ui/TransactionForm.tsx`
- `frontend/src/views/transactions/ui/{TransactionsView,MonthSwitcher,NewTransactionButton}.tsx`
- `frontend/src/widgets/transactions-kpi/ui/TransactionsKpi.tsx`

---

## Ограничения

- **Никаких новых зависимостей** — `springdoc-openapi-starter-webmvc-ui:2.6.0` уже в `build.gradle.kts:59`.
- **Не менять поведение** — только добавление JavaDoc/TSDoc/аннотаций.
- **Не трогать другие модули** (auth, user, category) — отдельный scope.
- **Соблюдать FSD** — TSDoc только в публичных API (`index.ts`) и непосредственно у экспортируемых сущностей.
