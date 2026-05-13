# Frontend CLAUDE.md
Next.js 15 / React 19 / TypeScript. Architecture: Feature-Sliced Design (FSD).

## Commands
```bash
npm run dev          # http://localhost:3000
npm run build
npm run lint && npm run typecheck
```

## Rules — you MUST follow these
- **FSD import order** (top→bottom only): `app → views → widgets → features → entities → shared`
- **Data fetching**: Server Components fetch directly — no `useEffect` for data.
- **Mutations**: Server Actions only (`'use server'`). No API route handlers for mutations.
- **Components**: default Server Components. `"use client"` only for interactivity/browser APIs.
- **Forms**: `react-hook-form` + zod v4. Use `.issues`, NOT `.errors`. Resolver: `@hookform/resolvers/zod`.
- **Type safety**: No `any`. Use `unknown` + type guards.
- **Tailwind v4**: `@import "tailwindcss"` + `@theme {}` in `globals.css`. No `tailwind.config.ts`.
- **Backend calls**: always via `backendFetch` from `shared/api/http.ts` (server-only). Never call backend from browser.
- **Notifications**: `sonner` (`toast.error`, `toast.success`). `<Toaster />` already in root `layout.tsx`.
- **Path alias**: `@/*` → `src/*`.
- **UI Kit**: shadcn/ui installed manually in `shared/ui/`. Imports use `@/shared/lib/cn`. No shadcn CLI.

## FSD Structure
```
src/
  app/
    (auth)/               # Public routes: /login, /register
    (authenticated)/      # Protected routes (layout fetches /users/me)
      transactions/       # /transactions — main page
      categories/         # /categories
      profile/            # /profile
    layout.tsx            # Root layout (<Toaster />)
    page.tsx              # redirect → /transactions
    globals.css           # Tailwind v4 (@import + @theme)

  views/                  # Page-level Server Component compositions
    login/ui/LoginPage.tsx
    register/ui/RegisterPage.tsx
    transactions/ui/TransactionsView.tsx + MonthSwitcher.tsx + NewTransactionButton.tsx
    categories/ui/CategoriesView.tsx
    profile/ui/ProfileView.tsx

  widgets/
    sidebar-nav/ui/SidebarNav.tsx       # aside nav, user card, logout
    transactions-kpi/ui/TransactionsKpi.tsx  # 3 KPI cards: Доходы/Расходы/Баланс

  features/
    auth/
      login-form/         # RHF form + login.action.ts
      register-form/      # RHF form + register.action.ts
      logout/             # logout.action.ts
    category-form/        # CategoryCreateForm + create/delete actions
    transaction-form/     # TransactionForm + transaction.action.ts

  entities/
    user/model/types.ts
    category/model/types.ts + icons.ts
    category/ui/CategoryCard.tsx
    transaction/model/types.ts
    transaction/ui/Amount.tsx + TransactionRow.tsx

  shared/
    api/
      http.ts             # backendFetch (server-only)
      endpoints.ts        # API URL constants
      dto.ts              # PagedResponse<T> and other shared DTOs
      problem.ts          # RFC 7807 error parsing
    config/env.ts         # env vars (BACKEND_INTERNAL_URL etc.)
    lib/cn.ts             # clsx + tailwind-merge
    lib/formatDate.ts     # date formatting helpers
    ui/                   # shadcn primitives: button, card, form, input, label, select, skeleton, sonner

middleware.ts             # Route protection — redirects unauthenticated to /login
```

## Auth Flow
- `accessToken` in HttpOnly cookie, `refreshToken` in HttpOnly cookie.
- `middleware.ts` checks `accessToken` presence — redirects to `/login` if missing.
- Server Actions proxy requests via `backendFetch` which reads cookies server-side.

## Feature slice structure convention
Each feature/entity/widget slice follows:
```
<slice>/
  api/        # Server Actions or fetch helpers
  model/      # types, schemas (zod), store
  ui/         # React components
  index.ts    # public barrel export
```
Only export through `index.ts`. Never import internals of another slice directly.
