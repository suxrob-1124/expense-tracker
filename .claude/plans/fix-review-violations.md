# План исправления нарушений из код-ревью REVIEW.md

## Context

Ревью по правилам `REVIEW.md` выявило 7 нарушений: 5 в backend (архитектура auth-модуля, транзакционность, Liquibase rollback, concurrency) и 2 во frontend (Zod v4 API, неточная денежная арифметика). Цель — устранить все нарушения, сохранив тесты `./scripts/smoke-test.sh` зелёными.

Окончательный файл плана нужно переместить из `~/.claude/plans/vivid-sprouting-phoenix.md` в `expense-tracker/.claude/plans/` после выхода из plan mode (в plan mode редактирование других файлов запрещено).

---

## 1. Backend — убрать импорт `UserQueryService` из `AuthController`

**Подход:** удалить поле `user` из `AuthResponse`. Frontend уже подгружает пользователя через `/users/me` в `(authenticated)/layout.tsx` — поле избыточно.

**Файлы:**

`backend/.../dto/auth/AuthResponse.java`
```java
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
```
- Удалить импорт `UserResponse`.

`backend/.../controller/auth/AuthController.java`
- Удалить импорты `UserQueryService`, `UserResponse`.
- Удалить поле `userQueryService` и аргумент конструктора.
- В `login()` и `refresh()`:
  ```java
  return ResponseEntity.ok(new AuthResponse(
          tokens.accessToken(), "Bearer", tokens.accessTtlSeconds()));
  ```

**Frontend:** проверить `features/auth/login-form/api/login.action.ts` и `register-form/api/register.action.ts`; если читают `AuthResponse.user` — удалить эти места. `(authenticated)/layout.tsx` продолжает работать как раньше.

---

## 2. Backend — `@Transactional` на `AuthService`

**Файл:** `backend/.../service/auth/AuthService.java`

```java
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    ...
    @Transactional(readOnly = true)
    public LoginTokens login(...) { ... }   // только чтение + событие

    @Transactional
    public LoginTokens refresh(...) { ... }  // запись в blacklist

    @Transactional
    public void revokeRefreshToken(...) { ... }
}
```
Дефолт класс-уровня `@Transactional`, у `login()` — `readOnly = true`.

---

## 3 + 4. Backend — добавить `<rollback>` в Liquibase changesets 001 и 002

**Важно:** rollback-блоки не участвуют в checksum'е changeset'а — добавлять их к уже применённым changeset'ам безопасно.

`backend/.../db/changelog/changes/20260506-001-create-users-table.xml` (внутри `<changeSet id="20260506-001">`, после `<createIndex>`):
```xml
<rollback>
    <dropIndex tableName="users" indexName="idx_users_email_hash"/>
    <dropTable tableName="users"/>
</rollback>
```

`backend/.../db/changelog/changes/20260506-002-create-audit-events-table.xml` (внутри `<changeSet id="20260506-002">`, после индексов):
```xml
<rollback>
    <dropIndex tableName="audit_events" indexName="idx_audit_events_occurred_at"/>
    <dropIndex tableName="audit_events" indexName="idx_audit_events_user_id"/>
    <dropTable tableName="audit_events"/>
</rollback>
```

---

## 5. Backend — `@Version` на `RevokedTokenJti`

**Файл:** `backend/.../domain/RevokedTokenJti.java`
```java
import jakarta.persistence.Version;

@Version
@Column(name = "version", nullable = false)
private Long version;

public Long getVersion() { return version; }
```

**Новый changeset:** `backend/.../db/changelog/changes/20260513-006-add-version-to-revoked-token-jtis.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">
    <changeSet id="20260513-006" author="system">
        <addColumn tableName="revoked_token_jtis">
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
        <rollback>
            <dropColumn tableName="revoked_token_jtis" columnName="version"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Регистрация:** добавить include в `backend/.../db/changelog/db.changelog-master.xml` (если используется master-файл с include-списком).

---

## 6. Frontend — Zod v4: `{ message }` → `{ error }`

**Файл:** `frontend/src/features/transaction-form/model/schema.ts:10`
```ts
date: z.string().datetime({ error: 'Введите корректную дату' }),
```
Образец корректной формы — `entities/category/model/schema.ts:7`.

---

## 7. Backend + Frontend — агрегация сумм на backend

**Архитектурное решение:** убрать `parseFloat` из frontend. Backend возвращает уже посчитанные `income`/`expense`/`balance` как `BigDecimal` (scale=4), формат сериализации — строка через `@JsonFormat(shape = STRING)` или дефолтный конфиг проекта (используется уже для `Transaction.amount`).

### Backend

**Новый DTO** `backend/.../dto/transaction/TransactionSummaryResponse.java`:
```java
public record TransactionSummaryResponse(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance
) {}
```

**Расширение `TransactionRepository`:**
```java
@Query("""
       SELECT t.type AS type, COALESCE(SUM(t.amount), 0) AS total
       FROM Transaction t
       WHERE t.userId = :userId AND t.date >= :from AND t.date < :to
       GROUP BY t.type
       """)
List<TransactionTotalProjection> sumByTypeForPeriod(UUID userId, Instant from, Instant to);
```
+ простой projection-интерфейс (`TransactionType getType(); BigDecimal getTotal();`).

**Метод в `TransactionQueryService`:**
```java
public TransactionSummaryResponse summarize(UUID userId, Integer month, Integer year) {
    YearMonth period = (month != null && year != null) ? YearMonth.of(year, month) : YearMonth.now(ZoneOffset.UTC);
    Instant from = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    BigDecimal income = BigDecimal.ZERO;
    BigDecimal expense = BigDecimal.ZERO;
    for (var row : transactionRepository.sumByTypeForPeriod(userId, from, to)) {
        if (row.getType() == TransactionType.INCOME) income = row.getTotal();
        else if (row.getType() == TransactionType.EXPENSE) expense = row.getTotal();
    }
    income = income.setScale(4, RoundingMode.UNNECESSARY);
    expense = expense.setScale(4, RoundingMode.UNNECESSARY);
    return new TransactionSummaryResponse(income, expense, income.subtract(expense));
}
```

**Endpoint в `TransactionController`:**
```java
@GetMapping("/summary")
public ResponseEntity<TransactionSummaryResponse> summary(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) Integer month,
        @RequestParam(required = false) Integer year) {
    return ResponseEntity.ok(transactionQueryService.summarize(principal.userId(), month, year));
}
```

### Frontend

**`shared/api/endpoints.ts`** — добавить:
```ts
transactions: {
  ...,
  summary: (month?: number, year?: number) =>
    month != null && year != null
      ? `/api/v1/transactions/summary?month=${month}&year=${year}`
      : '/api/v1/transactions/summary',
}
```

**`shared/api/dto.ts`** — добавить тип:
```ts
export interface TransactionSummaryResponse {
  income: string
  expense: string
  balance: string
}
```

**`views/transactions/ui/TransactionsView.tsx`:**
- Заменить вычисление `income`/`expense` через `parseFloat + reduce` на запрос `backendFetch(API.transactions.summary(month, year))`.
- Третий промис в `Promise.all`.
- `TransactionsKpi` получает `income`, `expense`, `balance` как `string`.

**`widgets/transactions-kpi/ui/TransactionsKpi.tsx`:**
- Props: `{ income: string; expense: string; balance: string }`.
- `fmt(value: string)` форматирует через `new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(Number(value))` — округление только для отображения; для логики ветвления цвета используем `balance.startsWith('-')` (а не `balance >= 0`).
- Знак баланса: `const isNegative = balance.startsWith('-')`.

---

## Порядок применения

1. Backend архитектура (1, 2).
2. Backend Liquibase rollback (3, 4).
3. Backend `@Version` + новый changeset (5).
4. Backend summary endpoint (7 backend часть).
5. Frontend: Zod (6), TransactionsView + TransactionsKpi на summary endpoint (7 frontend часть).

## Verification

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
./gradlew build -x test                                  # backend компилируется
docker compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local'  # changeset 006 применился
cd frontend && npm run lint && npm run typecheck && npm run build
./scripts/smoke-test.sh                                  # ожидаем 35/35
```

**Ручные сценарии:**
1. Login → редирект на `/transactions` → SidebarNav показывает имя/email (значит `/users/me` работает).
2. Refresh access-токена (или подождать истечения) → 200, новый access; повторный logout → 401 на refresh.
3. На `/transactions` создать 3 транзакции с дробями (`10.10`, `0.20`, `0.10`) — суммы Доходов/Расходов/Баланса в KPI точные.
4. Создать транзакцию с пустой/невалидной датой — сообщение Zod "Введите корректную дату".
5. SQL: `SELECT version FROM revoked_token_jtis LIMIT 1;` — колонка существует.

## Критические файлы

**Backend:**
- `controller/auth/AuthController.java`
- `dto/auth/AuthResponse.java`
- `service/auth/AuthService.java`
- `domain/RevokedTokenJti.java`
- `db/changelog/changes/20260506-001-create-users-table.xml`
- `db/changelog/changes/20260506-002-create-audit-events-table.xml`
- `db/changelog/changes/20260513-006-add-version-to-revoked-token-jtis.xml` (новый)
- `db/changelog/db.changelog-master.xml` (include нового changeset)
- `dto/transaction/TransactionSummaryResponse.java` (новый)
- `repository/TransactionRepository.java`
- `service/transaction/TransactionQueryService.java`
- `controller/transaction/TransactionController.java`

**Frontend:**
- `features/transaction-form/model/schema.ts`
- `views/transactions/ui/TransactionsView.tsx`
- `widgets/transactions-kpi/ui/TransactionsKpi.tsx`
- `shared/api/endpoints.ts`
- `shared/api/dto.ts`
- `features/auth/login-form/api/login.action.ts`, `register-form/api/register.action.ts` (проверить использование `AuthResponse.user`)
