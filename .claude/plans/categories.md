# План реализации модуля Categories

## Контекст

В существующем приложении (Spring Boot 3.4 / Java 21) уже реализованы шаги 1–4: User-модуль, Auth-модуль, Security (JWT), Audit, шифрование PII. Сейчас требуется добавить модуль **Categories** — справочник пользовательских категорий расходов. Каждая категория принадлежит конкретному пользователю; чужие категории недоступны ни на чтение, ни на запись. Модуль должен повторить архитектуру User-модуля: CQRS-разделение сервисов, MapStruct, Records-DTO, Jakarta Validation, RFC 7807 для ошибок, Liquibase для схемы.

Реализация должна точно следовать конвенциям, описанным в `CLAUDE.md` (пакетная структура, `@Version`, аудит, `@PreAuthorize` на сервисах, no `ddl-auto`).

---

## Архитектура

### Пакетная структура (повторяет User-модуль)

```
com.company.expensetracker
├── domain/Category.java
├── repository/CategoryRepository.java
├── dto/category/
│   ├── CategoryRequest.java       (create/update — один shared record)
│   └── CategoryResponse.java
├── service/category/
│   ├── CategoryMapper.java        (MapStruct)
│   ├── CategoryCommandService.java
│   └── CategoryQueryService.java
└── controller/category/CategoryController.java
```

### Поля сущности `Category`

| Поле | Тип | Ограничения |
|---|---|---|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = UUID)` |
| `name` | `String` | `NOT NULL`, длина 1–64 |
| `color` | `String` | `NOT NULL`, hex `#RRGGBB` (regex), длина 7 |
| `icon` | `String` | `NOT NULL`, длина 1–32 (emoji или текст) |
| `userId` | `UUID` | `NOT NULL`, индекс, FK → `users(id)` `ON DELETE CASCADE` |
| `version` | `Long` | `@Version` (обязательно по правилам проекта) |
| audit-поля | — | наследуются от `BaseEntity` |

`userId` хранится как простой `UUID` (без `@ManyToOne`) — User и Category изолированы по модулям; обращений к `User` напрямую быть не должно.

### Уникальность

Уникальная пара `(user_id, lower(name))` — один пользователь не может завести две категории с одинаковым именем (case-insensitive). Реализуется уникальным индексом по `user_id` и выражению `LOWER(name)`.

---

## API

База: `/api/v1/categories`. Все эндпоинты требуют JWT (роль `USER`).

| Метод | Путь | Описание | Статус |
|---|---|---|---|
| `POST` | `/` | Создать категорию | 201 + `Location` |
| `GET` | `/` | Список категорий текущего пользователя | 200 |
| `GET` | `/{id}` | Получить категорию по id | 200 / 404 |
| `PUT` | `/{id}` | Обновить категорию | 200 |
| `DELETE` | `/{id}` | Удалить категорию | 204 |

**Ownership-проверка:** во всех операциях по `id` сервис сначала ищет сущность, потом сравнивает `category.userId` с `principal.userId()`. Несовпадение → `ResponseStatusException(FORBIDDEN)` (или `NOT_FOUND`, чтобы не утекала информация о существовании — выбираем `NOT_FOUND`).

### DTO (Java Records)

```java
public record CategoryRequest(
    @NotBlank @Size(min=1, max=64) String name,
    @NotBlank @Pattern(regexp="^#[0-9A-Fa-f]{6}$") String color,
    @NotBlank @Size(min=1, max=32) String icon
) {}

public record CategoryResponse(
    UUID id, String name, String color, String icon,
    Instant createdAt, Instant updatedAt
) {}
```

### Маппер

`@Mapper(componentModel = "spring")` — `toResponse(Category)`, `toEntity(CategoryRequest)` (без id/userId), `updateEntity(@MappingTarget Category, CategoryRequest)` для PUT.

### Сервисы (CQRS)

**`CategoryQueryService`** (`@Service @Transactional(readOnly = true)`, `@PreAuthorize("hasRole('USER')")`):
- `List<CategoryResponse> findAllByUserId(UUID userId)`
- `CategoryResponse findByIdForUser(UUID id, UUID userId)` — кидает `NOT_FOUND` если не найдено или принадлежит другому

**`CategoryCommandService`** (`@Service @Transactional`, `@PreAuthorize("hasRole('USER')")`):
- `CategoryResponse create(UUID userId, CategoryRequest req)` — проверка уникальности имени → `CONFLICT`
- `CategoryResponse update(UUID id, UUID userId, CategoryRequest req)` — ownership + уникальность
- `void delete(UUID id, UUID userId)` — ownership

Контроллер берёт `userId` из `@AuthenticationPrincipal UserPrincipal principal` (`principal.userId()`) и передаёт в сервис — `@PreAuthorize` остаётся декларативным, фактическая ownership-логика в сервисах.

### Репозиторий

```java
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByUserIdOrderByNameAsc(UUID userId);
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
```

### Обработка ошибок

Уже работающий `GlobalExceptionHandler` покрывает:
- `MethodArgumentNotValidException` → 400 + поля
- `ResponseStatusException(NOT_FOUND/CONFLICT)` → 404/409 RFC 7807
- `OptimisticLockException` (при `@Version`) → 409

Новых обработчиков не нужно.

---

## Liquibase

Файл: `backend/src/main/resources/db/changelog/changes/20260506-003-create-categories-table.xml`. Master changelog подхватит автоматически (`<includeAll>`).

Содержимое changeset (id `20260506-003`, author `system`):
1. `createTable categories`: `id UUID PK`, `name VARCHAR(64) NOT NULL`, `color VARCHAR(7) NOT NULL`, `icon VARCHAR(32) NOT NULL`, `user_id UUID NOT NULL`, `version BIGINT NOT NULL DEFAULT 0`, audit-колонки (`created_at`, `updated_at`, `created_by`, `updated_by`).
2. `addForeignKeyConstraint`: `user_id` → `users(id)`, `onDelete=CASCADE`.
3. `createIndex` на `user_id` (не unique).
4. `createIndex` unique на `(user_id, LOWER(name))` через `<sql>` (Postgres-функциональный индекс):
   `CREATE UNIQUE INDEX idx_categories_user_name_lower ON categories (user_id, LOWER(name));`
5. `rollback` секции для каждой операции.

---

## Security

В `SecurityConfig` `/api/v1/categories/**` уже попадает под `.anyRequest().authenticated()` — отдельных правил **не добавляем**. Защита роли — через `@PreAuthorize("hasRole('USER')")` в сервисах (как в User-модуле).

---

## Критические файлы для модификации/создания

**Создать:**
- `backend/src/main/resources/db/changelog/changes/20260506-003-create-categories-table.xml`
- `backend/src/main/java/com/company/expensetracker/domain/Category.java`
- `backend/src/main/java/com/company/expensetracker/repository/CategoryRepository.java`
- `backend/src/main/java/com/company/expensetracker/dto/category/CategoryRequest.java`
- `backend/src/main/java/com/company/expensetracker/dto/category/CategoryResponse.java`
- `backend/src/main/java/com/company/expensetracker/service/category/CategoryMapper.java`
- `backend/src/main/java/com/company/expensetracker/service/category/CategoryQueryService.java`
- `backend/src/main/java/com/company/expensetracker/service/category/CategoryCommandService.java`
- `backend/src/main/java/com/company/expensetracker/controller/category/CategoryController.java`

**Не трогаем:** `SecurityConfig`, `GlobalExceptionHandler`, `BaseEntity`, `UserPrincipal`, `db.changelog-master.xml`.

**Переиспользуем:**
- `BaseEntity` (`domain/BaseEntity.java`) — наследование для audit + `@EntityListeners`.
- `UserPrincipal#userId()` (`security/UserPrincipal.java`) — получение текущего пользователя.
- Конвенции маппера и валидации — из `service/user/UserMapper.java`, `dto/user/RegisterRequest.java`.

---

## Чек-лист реализации

### БД
- [x] Создать changeset `20260506-003-create-categories-table.xml` (createTable + FK + индексы + rollback).
- [x] Запустить приложение локально, убедиться, что Liquibase применил миграцию без ошибок.

### Domain / Repository
- [x] `Category` extends `BaseEntity`, поля + `@Version`, `@GeneratedValue(UUID)`.
- [x] `CategoryRepository` с методами `findAllByUserIdOrderByNameAsc`, `findByIdAndUserId`, `existsByUserIdAndNameIgnoreCase`.

### DTO / Mapper
- [x] Record `CategoryRequest` с `@NotBlank`, `@Size`, `@Pattern` (hex color).
- [x] Record `CategoryResponse` с id, полями, audit-датами.
- [x] `CategoryMapper` (`@Mapper(componentModel="spring")`): `toResponse`, `toEntity`, `updateEntity` (`@MappingTarget`).

### Services
- [x] `CategoryQueryService` (`@Transactional(readOnly=true)` + `@PreAuthorize("hasRole('USER')")`).
- [x] `CategoryCommandService` (`@Transactional` + `@PreAuthorize`), методы `create`/`update`/`delete` с ownership-проверкой и проверкой уникальности имени.
- [x] Конфликты имён → `ResponseStatusException(CONFLICT)`. Чужая/несуществующая категория → `NOT_FOUND`.

### Controller
- [x] `CategoryController` на `/api/v1/categories`: 5 эндпоинтов, `@AuthenticationPrincipal UserPrincipal`, `ResponseEntity<…>`, `@Valid` на bodies.
- [x] `POST` возвращает 201 + `Location: /api/v1/categories/{id}`.
- [x] `DELETE` возвращает 204.

### Verification
- [x] `./gradlew build -x test` проходит.
- [x] `./gradlew test` (Testcontainers) проходит — `NO-SOURCE` (тест-классы ещё не написаны, BUILD SUCCESSFUL).
- [x] Smoke test вручную через curl с JWT:
  - регистрация + логин → получить access token
  - `POST /api/v1/categories` с валидным body → 201
  - `GET /api/v1/categories` → массив с одной категорией
  - `PUT /api/v1/categories/{id}` → 200
  - `POST` с дубликатом name → 409 RFC 7807
  - `GET /api/v1/categories/{foreign-id}` (id чужого пользователя) → 404
  - `DELETE` → 204, повторный `GET` → 404
  - запрос без JWT → 401

### Документация
- [x] Обновить таблицу «Implementation Status» в `CLAUDE.md`: добавить строку «Шаг 5 — Category модуль».
