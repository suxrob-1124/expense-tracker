# Шаблон реализации: [Название модуля]

## Контекст и Цель
## Слой Данных (Persistence)
- **БД:** Создать changeset Liquibase в `db/changelog/changes/`.
- **Entity:** Создать сущность в `domain/`, наследуя `BaseEntity` (для аудита).
- **Контроль версий:** Обязательное использование `@Version` (Optimistic Locking).
- **Repository:** Интерфейс в `repository/`, обязательно включающий фильтрацию по `userId`.

## Backend Logic (CQRS-lite)
- **DTOs:** Использовать Java Records для Request и Response.
- **Mapping:** Создать MapStruct маппер в пакете `service/[module]/`.
- **Services:**
    - **QueryService:** `@Transactional(readOnly = true)`, логика получения данных.
    - **CommandService:** `@Transactional`, логика изменения (Create, Update, Delete).
- **Validation:** Аннотации Jakarta Validation в Request DTO.
- **Controller:** REST-контроллер в `controller/[module]/` с использованием `@AuthenticationPrincipal`.

## Frontend (FSD Layers)
- **Shared/Entities:** Описание типов (Interface) и API-клиента на основе бэкенд-DTO.
- **Features:** - Логика взаимодействия (Server Actions) в `api/[name].action.ts`.
    - Схемы валидации (Zod), зеркальные бэкенд-валидации.
- **Views:** Страницы модуля в слое `views/`.
- **Components:** Использование UI-примитивов из `shared/ui/` (shadcn).

## Безопасность и Ownership
- **Проверка владения:** Любая операция по `id` должна проверять, что `entity.userId == currentUserId`.
- **Ошибки:** Использовать RFC 7807 (Problem Details) для возврата ошибок (404 вместо 403, если объект не найден или чужой).

## Критерии готовности (DoD)
- [ ] Liquibase миграция успешно прошла (есть rollback секция).
- [ ] Backend: `./gradlew build` проходит без ошибок.
- [ ] Frontend: `npm run build` проходит, типизация `shared/api` консистентна.
- [ ] Smoke-тест: CRUD операции проверены вручную или скриптом.
- [ ] Ownership: Попытка доступа к чужому ID возвращает 404.
