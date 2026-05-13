# Задание: Реализация модуля Транзакций (Transactions)

## Контекст и Цель
Реализовать центральный модуль учёта доходов и расходов. Сущность `Transaction` связывает пользователя, категорию и денежную операцию.

## Слой Данных (Persistence)
- **БД:** Создать changeset Liquibase в `backend/src/main/resources/db/changelog/changes/`.
  - Таблица `transactions`: `id (UUID PK)`, `amount (DECIMAL(19,2) NOT NULL)`, `type (VARCHAR(16) NOT NULL)` (INCOME/EXPENSE), `description (VARCHAR(255))`, `date (TIMESTAMP WITH TIME ZONE NOT NULL)`, `category_id (UUID NOT NULL)`, `user_id (UUID NOT NULL)`, `version (BIGINT NOT NULL)`, + audit-колонки.
  - Индексы: `user_id`, `category_id`, `date`. Foreign key к `users` и `categories` с `ON DELETE CASCADE`.
- **Entity:** `domain/Transaction.java` (extends `BaseEntity`).
- **Контроль версий:** `@Version` на поле `version`.

## Backend Logic (CQRS-lite)
- **DTOs:** Java Records `TransactionRequest` (для создания/обновления) и `TransactionResponse`.
- **Mapping:** `service/transaction/TransactionMapper.java` (MapStruct).
- **Services:**
    - **TransactionQueryService:**
        - `findAllForUser(userId, month, year)` — фильтрация по периоду.
        - `findByIdForUser(id, userId)`.
    - **TransactionCommandService:**
        - `create`, `update`, `delete` (с обязательной проверкой владения).
- **Validation:** `amount` > 0, `type` не null, `date` не null.
- **Controller:** `controller/transaction/TransactionController.java` на пути `/api/v1/transactions`.

## Frontend (FSD Layers)
- **Shared/Entities:**
    - Обновить `shared/api/dto.ts` и `endpoints.ts`.
    - Создать `entities/transaction` для базовых типов и простых UI-элементов (например, компонент отображения суммы).
- **Features:**
    - `features/transaction-form`: форма создания/редактирования (RHF + Zod).
    - `api/transaction.action.ts`: Server Actions для всех методов.
- **Views:** Страница `views/transactions` (список транзакций с фильтром по месяцам).

## Безопасность и Ownership
- **Проверка владения:** Пользователь может оперировать только своими транзакциями.
- **Связь с категориями:** При создании транзакции проверить, что `categoryId` принадлежит текущему пользователю. Если нет — возвращать 404 (или 400 Validation Error).

## Критерии готовности (DoD)
- [ ] Liquibase миграция успешно применена.
- [ ] Backend: `./gradlew build` проходит.
- [ ] API эндпоинты соответствуют спецификации:
    - `POST /api/v1/transactions`
    - `GET /api/v1/transactions?month=X&year=Y`
    - `GET /api/v1/transactions/{id}`
    - `PATCH /api/v1/transactions/{id}`
    - `DELETE /api/v1/transactions/{id}`
- [ ] Frontend: Реализованы Server Actions и базовая верстка списка.
- [ ] Ownership: Протестирована невозможность создать транзакцию в чужой категории.
