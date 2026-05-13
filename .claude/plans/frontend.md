# Шаг 6 — Frontend Setup & Auth Pages (FSD)

> **Статус**: план готов к выполнению. Реализация — после явного согласия.

## 1. Контекст

Бэкенд готов до Шага 4 (User/Auth модули, JWT + refresh-cookie, RFC 7807). Frontend — пустой каркас Next.js 15.5 + React 19 + Tailwind 4 (`@tailwindcss/postcss`), TypeScript strict, alias `@/*`. Каркас не пересоздаём — расширяем.

Цель шага: production-ready фронтенд по архитектуре **Feature-Sliced Design**, страницы `/login` и `/register` с zod-валидацией, Server Actions для общения с Spring Boot, корректное проксирование HttpOnly cookies, RFC 7807 ошибки → Toast.

## 2. Архитектура FSD

Слои сверху вниз (импорт допустим только в нижестоящие):
`app → pages → widgets → features → entities → shared`.

```
src/
  app/                          # Next.js App Router
    (auth)/
      layout.tsx                # центрирование + <Toaster />
      login/page.tsx            # → <LoginPage />
      register/page.tsx         # → <RegisterPage />
    dashboard/page.tsx          # заглушка для редиректа после auth
    layout.tsx
    globals.css                 # @theme + shadcn-токены
  pages/
    login/ui/LoginPage.tsx
    register/ui/RegisterPage.tsx
  widgets/                      # пока пусто (.gitkeep)
  features/
    auth/
      login-form/
        ui/LoginForm.tsx        # 'use client' + RHF
        model/schema.ts         # zod
        api/login.action.ts     # 'use server'
        index.ts
      register-form/            # аналогично
  entities/
    user/
      model/types.ts            # реэкспорт UserResponse
      index.ts
  shared/
    api/
      endpoints.ts              # пути backend
      dto.ts                    # типы 1:1 c Java records
      problem.ts                # ProblemDetail + парсер
      http.ts                   # server-only fetch + Set-Cookie прокси
    config/env.ts               # типобезопасные env
    lib/cn.ts                   # clsx + tailwind-merge
    ui/                         # shadcn примитивы (ручная установка)
      button.tsx, input.tsx, label.tsx,
      card.tsx, form.tsx, sonner.tsx
middleware.ts                   # защита /dashboard, /login, /register
```

Правила импорта проверяются дисциплинарно (без линтера на этом этапе).

## 3. Контракты API ↔ TypeScript DTO

| Endpoint | Method | Status | Request DTO | Response DTO |
|---|---|---|---|---|
| `/api/v1/auth/login` | POST | 200 | `LoginRequest` | `AuthResponse` |
| `/api/v1/auth/refresh` | POST | 200 | — | `AuthResponse` |
| `/api/v1/auth/logout` | POST | 204 | — | — |
| `/api/v1/users/register` | POST | 201 | `RegisterRequest` | `UserResponse` |
| `/api/v1/users/me` | GET | 200 | — | `UserResponse` |
| `/api/v1/users/me/password` | POST | 204 | `ChangePasswordRequest` | — |

```ts
// shared/api/dto.ts (1:1 с Java records)
export interface LoginRequest { email: string; password: string }
export interface RegisterRequest {
  email: string
  password: string                // backend: @Size(min=12, max=128)
  firstName: string               // @Size(max=100)
  lastName: string                // @Size(max=100)
}
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string             // 12..128
}
export interface UserResponse {
  id: string                      // UUID
  email: string
  firstName: string
  lastName: string
  role: string                    // 'USER' | 'ADMIN'
  createdAt: string               // Instant ISO
}
export interface AuthResponse {
  accessToken: string
  tokenType: string               // 'Bearer'
  expiresInSeconds: number
  user: UserResponse
}

// shared/api/problem.ts (RFC 7807)
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail?: string
  instance?: string
}
```

Refresh-cookie от backend: `refreshToken`, HttpOnly, Secure, SameSite=Strict, **Path=`/api/v1/auth`** — проксируется через Server Action с тем же Path.

CORS бэка: `http://localhost:3000`, `allowCredentials=true`.

## 4. Tailwind 4 + shadcn/ui — особенности

В Tailwind v4 **нет** `tailwind.config.ts`. Все темы — в CSS:

```css
/* app/globals.css */
@import "tailwindcss";
@plugin "tailwindcss-animate";

@theme {
  --color-background: hsl(0 0% 100%);
  --color-foreground: hsl(222.2 84% 4.9%);
  --color-primary: hsl(222.2 47.4% 11.2%);
  /* ... остальные shadcn-токены */
  --radius: 0.5rem;
}
```

shadcn-CLI таргетит v3 → ставим **вручную**:
1. Не запускаем `npx shadcn init` (создаст лишний `components.json` и `lib/utils.ts` вне FSD).
2. Копируем 6 примитивов с shadcn-сайта в `src/shared/ui/`.
3. Импорты `@/lib/utils` → `@/shared/lib/cn`.

Если адаптация Tailwind 4 даст артефакты со стилями — fallback на минимальную ручную имплементацию Button/Input/Card.

## 5. Server Actions + HttpOnly cookies

Поток: **browser → Server Action (Next.js) → backend `:8080`**. Прямых fetch'ей из браузера к `:8080` нет → single-origin, CORS не задействован в проде.

`shared/api/http.ts` (server-only):
```ts
import { cookies } from 'next/headers'

export async function backendFetch(path: string, init: RequestInit & {
  forwardRefreshCookie?: boolean
}) {
  const headers = new Headers(init.headers)
  if (init.forwardRefreshCookie) {
    const rt = (await cookies()).get('refreshToken')?.value
    if (rt) headers.set('Cookie', `refreshToken=${rt}`)
  }
  headers.set('Content-Type', 'application/json')
  return fetch(`${env.BACKEND_INTERNAL_URL}${path}`, { ...init, headers })
}
```

Логика login/register Server Action:
1. Валидация zod (двойная защита поверх RHF).
2. `backendFetch(...)` к нужному endpoint.
3. На успех:
   - читаем `Set-Cookie` из ответа → парсим `refreshToken`, ставим через `cookies().set(...)` с Path=`/api/v1/auth`, HttpOnly, Secure, SameSite=Strict;
   - кладём `accessToken` из тела ответа в HttpOnly cookie с Path=`/`, Max-Age=`expiresInSeconds`;
   - `redirect('/dashboard')`.
4. На !ok: парсим `application/problem+json`, возвращаем `{ ok: false, problem }`. RHF / `useActionState` пробрасывает в Toast (`sonner`).

Logout: вызов `/api/v1/auth/logout` с проксированной refresh-cookie + локальная очистка обеих cookies + redirect на `/login`.

`middleware.ts`:
- `/dashboard` без `accessToken` → redirect `/login`.
- `/login`, `/register` с `accessToken` → redirect `/dashboard`.

## 6. Валидация (zod ⟷ backend)

```ts
// login.schema
email: z.string().email().min(1)
password: z.string().min(1)

// register.schema (паритет с @Size бэкенда)
email: z.string().email()
password: z.string().min(12, '≥12 символов').max(128)
firstName: z.string().max(100)
lastName: z.string().max(100)
```

## 7. Зависимости (npm install)

```
react-hook-form @hookform/resolvers zod
class-variance-authority @radix-ui/react-slot @radix-ui/react-label
sonner tailwindcss-animate
```

## 8. Чек-лист задач

### 8.1. Подготовка
- [x] `npm install` новых зависимостей.
- [x] Обновить `.env.local.example`: добавить `BACKEND_INTERNAL_URL=http://localhost:8080`.
- [x] Создать `src/shared/config/env.ts` (типобезопасное чтение env).

### 8.2. Shared-слой
- [x] `shared/lib/cn.ts` (clsx + tailwind-merge).
- [x] `shared/api/endpoints.ts` (константы путей).
- [x] `shared/api/dto.ts` (все DTO).
- [x] `shared/api/problem.ts` (тип + парсер + formatter).
- [x] `shared/api/http.ts` (server-only fetch + проксирование Cookie).
- [x] `shared/ui/`: button, input, label, card, form, sonner — вручную.

### 8.3. Глобальные стили
- [x] `app/globals.css` — `@theme` блок с shadcn-токенами + `@plugin "tailwindcss-animate"`.
- [x] `app/layout.tsx` — добавить глобальный `<Toaster />`.

### 8.4. Entities
- [x] `entities/user/model/types.ts` (реэкспорт `UserResponse`).
- [x] `entities/user/index.ts`.

### 8.5. Features (auth)
- [x] `features/auth/login-form/`: schema, action, ui, index.
- [x] `features/auth/register-form/`: schema, action, ui, index.

### 8.6. Pages (FSD)
- [x] `pages/login/ui/LoginPage.tsx`.
- [x] `pages/register/ui/RegisterPage.tsx`.

### 8.7. App Router
- [x] `app/(auth)/layout.tsx`.
- [x] `app/(auth)/login/page.tsx` → `<LoginPage />`.
- [x] `app/(auth)/register/page.tsx` → `<RegisterPage />`.
- [x] `app/dashboard/page.tsx` (заглушка + Logout-action).
- [x] **Примечание**: FSD-слой `pages/` переименован в `views/` — конфликт с Next.js Pages Router.

### 8.8. Middleware
- [x] `middleware.ts` (защита маршрутов + редиректы).

### 8.9. Документация
- [x] Обновить `CLAUDE.md`: раздел Frontend Architecture переписан под FSD, зафиксированы RHF/zod v4/shadcn/sonner, стратегия Server Actions + cookie-проксирование. Шаг 6 помечен ✅.

### 8.10. Verification (DoD)
- [x] `npm run typecheck` — 0 ошибок.
- [ ] `npm run lint` — запустить вручную.
- [x] `npm run build` — успешно (standalone output, 5 маршрутов, Middleware 34.3 kB).
- [x] E2E-проверка (`scripts/e2e-frontend.sh` — все 8 чеков прошли):
  - [x] Регистрация happy-path → 201 Created.
  - [x] Регистрация дубль email → 409 ProblemDetail (RFC 7807).
  - [x] Login happy-path → 200, accessToken + refreshToken cookie в ответе.
  - [x] `/users/me` с токеном → 200, email совпадает.
  - [x] Middleware: `/dashboard` без cookie → redirect `/login`.
  - [x] Middleware: `/login` с cookie → redirect `/dashboard`.
  - [x] Logout → 204 No Content.
  - [x] postgres + backend containers healthy.
  - [ ] Zod client-side (пароль <12) — проверяется только в браузере.

## 9. Риски и заметки

- **Tailwind v4 + shadcn**: shadcn таргетит v3. Используем CSS-переменные через `@theme`. Если стили поедут — fallback на ручные минимальные примитивы.
- **Refresh-cookie Path=`/api/v1/auth`**: Server Action для refresh обязан ходить именно туда, иначе кука не отправится.
- **`create-next-app` НЕ запускаем**: каркас уже есть, перезапись затрёт `next.config.js` (security headers).
- **`.gitkeep`**: в `widgets/` оставить, пока слой пуст.
