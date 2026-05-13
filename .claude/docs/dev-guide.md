# Developer Guide

Step-by-step recipes for the three most common changes: adding a new backend module, adding a new frontend feature, and adding a database migration. Each recipe lists the files you need to touch, in dependency order.

---

## Conventions you cannot skip

- **Commits**: Conventional Commits, e.g. `feat(transactions): add monthly summary endpoint`. Scopes: `auth | user | category | transactions | frontend | db | security`.
- **Branches**: `feat/<slug>`, `fix/<slug>`, `refactor/<slug>`, `docs/<slug>`, `chore/<slug>`. Branch from `main`. Squash-merge only.
- **Pre-commit**:
  ```bash
  cd backend  && ./gradlew build -x test
  cd frontend && npm run build && npm run lint && npm run typecheck
  ./scripts/smoke-test.sh
  ```
- **Documentation language**: JavaDoc / TSDoc / OpenAPI text is **English**. Always. Even when chatting with the user in Russian.
- **Type discipline**: no `any` in TS, no raw `Object` in Java.

---

## Recipe 1 — Add a new backend module

We will add a fictional `budgets` module (a `Budget` is a per-month spending cap per category). Mirror the structure of the `transactions` reference module.

### Step-by-step

1. **Database migration**  
   `backend/src/main/resources/db/changelog/changes/YYYYMMDD-NNN-create-budgets-table.xml`  
   See recipe 3 below.

2. **Domain entity** — `domain/Budget.java`
   - Extends `BaseEntity` (gives audit columns).
   - `@Version Long version` is mandatory.
   - Monetary fields → `BigDecimal` with `precision = 19, scale = 4`.
   - PII fields → annotate with `@Convert(converter = AesGcmStringConverter.class)`.

3. **Repository** — `repository/BudgetRepository.java`
   - Extends `JpaRepository<Budget, UUID>`.
   - Add custom methods only when needed; document non-trivial `@Query` methods.

4. **DTOs** — `dto/budget/`
   - Use **Java Records** only. Never classes.
   - Add `jakarta.validation` annotations (`@NotNull`, `@Positive`, `@Size`).
   - Add `@Schema` on the record class **and on every component**.
     - `requiredMode = REQUIRED` for `@NotNull` fields, `NOT_REQUIRED` for patch/optional.
     - State scale for `BigDecimal`, ISO-8601 for `Instant`, give a UUID example for `UUID`.

5. **MapStruct mapper** — `service/budget/BudgetMapper.java`
   - `@Mapper(componentModel = "spring")`.
   - Methods: `toResponse(entity)`, `toEntity(request, userId)`, `patchEntity(@MappingTarget entity, patch)`.
   - Never write manual mapping.

6. **CQRS services**
   - `service/budget/BudgetCommandService.java` — `@Transactional`, `@PreAuthorize("isAuthenticated()")`. Owns mutations. Reads the current `UserPrincipal` from `SecurityContextHolder`.
   - `service/budget/BudgetQueryService.java` — `@Transactional(readOnly = true)`. Owns reads. Validates pagination bounds (`0 ≤ page`, `1 ≤ size ≤ 50`).

7. **Controller** — `controller/budget/BudgetController.java`
   - `@RequestMapping("/api/v1/budgets")`.
   - Class-level annotations: `@Tag(name = "Budgets", description = "...")`, `@SecurityRequirement(name = "bearerAuth")`.
   - Each method: `@Operation(summary, description)` + `@ApiResponses` listing every realistic status (200/201/204/400/401/403/404/409) + `@Parameter` on path/query params.
   - Orchestration only — no business logic. Return `ResponseEntity`.

8. **Security wiring**
   - If the endpoint should be public, edit `config/SecurityConfig.java` to permit it. Otherwise default `authenticated()` applies.

9. **Tests**
   - Integration tests in `backend/src/test/java/...` using Testcontainers (Docker required).

10. **Smoke tests**
    - Append checks for the new endpoints to `scripts/smoke-test.sh`.

11. **Verify Swagger** — open `http://localhost:8080/swagger-ui/index.html`, confirm the new `Budgets` tag renders with descriptions, examples, and all response codes.

### Inter-module rules (enforced by review)
- A new module MUST NOT import another module's repositories. Cross-module communication is **events** or the **`UserDetailsService` SPI**.
- `@PreAuthorize` belongs on services, never on controllers.

---

## Recipe 2 — Add a new frontend feature

We will add a `budget-form` feature in `frontend/src/features/budget-form/`. Mirror `features/transaction-form/`.

### FSD checklist (imports flow top → bottom: `app → views → widgets → features → entities → shared`)

1. **Shared layer additions** (if backend types changed)
   - `shared/api/dto.ts` — add the TypeScript mirror of any new Java DTO. TSDoc must reference the Java record name, state field formats (e.g. `"decimal string, scale 4"`, `"ISO-8601 UTC instant"`), include an example.
   - `shared/api/endpoints.ts` — add helpers under `API.budgets.*`. TSDoc must state HTTP method + path + query-param semantics.

2. **Entity layer** — `entities/budget/` (only if the feature has reusable types or presentational components)
   ```
   entities/budget/
     model/types.ts       # one-line TSDoc per re-exported type
     ui/BudgetCard.tsx    # props interface + component, fully documented
     index.ts             # public barrel
   ```

3. **Feature slice** — `features/budget-form/`
   ```
   budget-form/
     api/
       create-budget.action.ts   # 'use server'
       update-budget.action.ts
     model/
       schema.ts                 # zod v4 — use .issues, NOT .errors
     ui/
       BudgetForm.tsx            # client component (uses react-hook-form)
     index.ts                    # ONLY public export surface
   ```
   - **Mutations** are Server Actions, never API route handlers.
   - Validation: `zodResolver` from `@hookform/resolvers/zod`. The same schema validates client- and server-side.
   - Backend calls go through `backendFetch` from `shared/api/http.ts`. Browser never calls the backend directly.
   - On success: `revalidatePath('/budgets')` + `toast.success(...)`. On error: `toast.error(...)`.
   - The Server Action's return type must be a discriminated union, e.g.
     ```ts
     { ok: true; data: BudgetResponse } | { ok: false; message: string; fieldErrors?: Record<string, string> }
     ```
     and every branch is described in TSDoc.

4. **Widget layer** (optional) — `widgets/budgets-kpi/` for page-independent composite blocks.

5. **View layer** — `views/budgets/ui/BudgetsView.tsx`
   - Server Component. Fetches its data directly in parallel via `Promise.all`.
   - TSDoc: list endpoints fetched, fallback values, page composition.

6. **Route page** — `app/(authenticated)/budgets/page.tsx`
   - Thin wrapper that imports `BudgetsView` from `views/budgets`. No data fetching here.

7. **Navigation** — add the link in `widgets/sidebar-nav/ui/SidebarNav.tsx`.

8. **Verify**
   - `npm run typecheck` → 0 errors.
   - `npm run build` → passes.
   - Hover on `createBudgetAction`, `BudgetResponse`, `<BudgetCard>` in your editor — TSDoc must render.

### Frontend rules you cannot skip

- Default to **Server Components**. `"use client"` only for interactivity or browser APIs.
- No `useEffect` for data fetching — Server Components fetch directly.
- Forms: `react-hook-form` + zod v4. Resolver: `@hookform/resolvers/zod`. Use `.issues`, not `.errors`.
- Types: no `any`. Use `unknown` + type guards.
- Tailwind v4: `@import "tailwindcss"` + `@theme {}` in `globals.css` only. **No `tailwind.config.ts`.**
- Path alias: `@/*` → `src/*`.
- Only export through `index.ts`. Never import another slice's internals directly.

---

## Recipe 3 — Add a database migration

Liquibase is the **only** way the schema changes — Hibernate runs with `ddl-auto: validate`.

### File location & naming

- Path: `backend/src/main/resources/db/changelog/changes/`
- Name: `YYYYMMDD-NNN-<short-description>.xml` (e.g. `20260520-007-create-budgets-table.xml`).
- The master changelog uses `<includeAll path="db/changelog/changes/" .../>` — your file is picked up automatically.

### Skeleton

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <changeSet id="20260520-007-create-table" author="alice">
        <createTable tableName="budgets">
            <column name="id" type="UUID">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="amount" type="DECIMAL(19,4)">
                <constraints nullable="false"/>
            </column>
            <column name="category_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP"><constraints nullable="false"/></column>
            <column name="updated_at" type="TIMESTAMP"><constraints nullable="false"/></column>
            <column name="created_by" type="VARCHAR(255)"><constraints nullable="false"/></column>
            <column name="updated_by" type="VARCHAR(255)"><constraints nullable="false"/></column>
        </createTable>

        <rollback>
            <dropTable tableName="budgets"/>
        </rollback>
    </changeSet>

    <changeSet id="20260520-007-add-fk-user" author="alice">
        <addForeignKeyConstraint
                baseTableName="budgets" baseColumnNames="user_id"
                referencedTableName="users" referencedColumnNames="id"
                constraintName="fk_budgets_user_id"
                onDelete="CASCADE"/>
        <rollback>
            <dropForeignKeyConstraint baseTableName="budgets" constraintName="fk_budgets_user_id"/>
        </rollback>
    </changeSet>

    <changeSet id="20260520-007-add-index-user-id" author="alice">
        <createIndex indexName="idx_budgets_user_id" tableName="budgets">
            <column name="user_id"/>
        </createIndex>
        <rollback>
            <dropIndex tableName="budgets" indexName="idx_budgets_user_id"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### Rules you cannot skip

- **Every `<changeSet>` MUST have a `<rollback>` block.** No exceptions.
- **One logical operation per changeset.** Splitting `createTable` / `addForeignKey` / `createIndex` keeps rollbacks granular.
- **Never modify a merged changeset.** Liquibase tracks them by hash — add a new changeset to amend.
- **Money** → `DECIMAL(19,4)`. Never `FLOAT`/`DOUBLE`/`NUMERIC` without scale.
- **Timestamps** → `TIMESTAMP` for auditing; `TIMESTAMP WITH TIME ZONE` when the column represents a user-supplied instant (like `transactions.date`).
- **Soft deletes vs FK cascade**: pick a side and stick to it. Existing tables use FK cascade.
- **Postgres-specific SQL** (e.g. `CREATE UNIQUE INDEX ... ON ... (user_id, LOWER(name))`) goes inside `<sql>` blocks with a matching `<sql>` rollback.

### Verify locally

```bash
docker compose up -d postgres
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'
# Watch the log for: "Liquibase: Successfully released change log lock"
```

Then connect with `psql` (`PGPASSWORD=postgres psql -h localhost -U postgres expense_tracker`) and inspect `\d budgets`.

---

## Quick reference

| Want to... | Touch these files |
|---|---|
| Add a REST endpoint | `controller/*` + `service/*Command|QueryService` + (maybe) `dto/*` |
| Add a DB column | new Liquibase changeset + entity field + (maybe) MapStruct mapper update |
| Add a UI page | `views/<slice>/` + `app/(authenticated)/<route>/page.tsx` + `widgets/sidebar-nav` |
| Add a form | `features/<slice>/` (`api/` + `model/schema.ts` + `ui/<X>Form.tsx`) |
| Expose a new DTO to the frontend | `shared/api/dto.ts` + `shared/api/endpoints.ts` (with TSDoc) |
| Add a domain event | `event/<Name>Event.java` + publisher in the producing service + `@TransactionalEventListener` consumer |
| Add a new role | `domain/Role.java` enum value + `SecurityConfig` rules + Liquibase data migration if needed |

## Where to look for reference implementations

- **Backend canonical example**: the `transactions` module (`controller/transaction/`, `service/transaction/`, `dto/transaction/`, changeset `20260507-004`).
- **Frontend canonical example**: the `transaction-form` feature + `views/transactions/` + `widgets/transactions-kpi/` + `entities/transaction/`.

Mimic their structure and tone — both layers are reviewed against these references.
