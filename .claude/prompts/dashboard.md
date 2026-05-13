# Задание: Dashboard (Главный экран)

## 🎯 Контекст и Цель
Создание центрального интерфейса пользователя для навигации по приложению и быстрого обзора последних финансовых операций.

## 💾 Слой Данных (Persistence)
- **БД:** Новых таблиц не требуется.
- **Entity:** Используются существующие `User` и `Transaction`.
- **Repository:** В `TransactionRepository` добавить поддержку пагинации: `findAllByUserIdOrderByDateDesc(UUID userId, Pageable pageable)`.

## 🏗 Backend Logic (CQRS-lite)
- **DTOs:** - `TransactionResponse` (существующий).
    - `PagedResponse<TransactionResponse>` (новый record для обертки данных пагинации).
- **Mapping:** Использовать существующий `TransactionMapper`.
- **Services:**
    - **TransactionQueryService:** Добавить метод `findLatest(userId, page, size)` с возвратом пагинированного списка.
    - **UserQueryService:** Использовать существующий метод получения текущего профиля.
- **Controller:** В `TransactionController` добавить эндпоинт `GET /api/v1/transactions/latest?page=0&size=10`.

## 🎭 Frontend (FSD Layers)
- **Shared:** UI-примитивы (Card, Button, Skeleton).
- **Entities:** - `user`: Компонент отображения имени профиля.
    - `transaction`: Компонент строки транзакции для списка.
- **Features:** Логика переключения страниц пагинации.
- **Widgets:** - `Sidebar/NavMenu`: Ссылки на `/transactions` и `/categories`.
    - `RecentTransactionsList`: Виджет списка из 10 последних записей с контроллером пагинации.
- **Views:** `DashboardView` в `src/views/dashboard`, собирающий виджеты в единый экран.

## 🛡 Безопасность и Ownership
- **Проверка владения:** Фильтрация транзакций строго по `principal.userId()`.
- **Ошибки:** RFC 7807 при сбоях получения данных профиля или списка.

## ✅ Критерии готовности (DoD)
- [ ] Backend: `./gradlew build` проходит, эндпоинт пагинации возвращает корректную страницу.
- [ ] Frontend: `npm run build` без ошибок, соблюдена вложенность FSD (`widgets` -> `entities`).
- [ ] UI: Отображается имя текущего пользователя.
- [ ] UI: Меню навигации активно и переключает маршруты.
- [ ] UI: Список транзакций ограничен 10 записями на страницу, пагинация работает.
- [ ] Smoke-test: Проверено получение последних 10 транзакций через curl.
- [ ] Ownership: Пользователь видит только свои транзакции на дашборде.