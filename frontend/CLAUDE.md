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
- **Documentation**: every public export from a slice MUST carry TSDoc — see *Documentation Rules* below. NO EXCEPTIONS.

## Documentation Rules — write it with the code (NOT later)

**Language**: ALL TSDoc/JSDoc MUST be in **English**. Always.

### What requires TSDoc (per FSD layer)
| Layer | What to document | Focus on |
|---|---|---|
| `shared/api/dto.ts` | every interface/type that mirrors a Java DTO | which Java record it mirrors, format of every field (e.g. "ISO-8601 UTC instant", "decimal string, scale 4"), realistic example |
| `shared/api/endpoints.ts` | every endpoint helper inside `API.*` | HTTP method + path, query-param semantics, default behaviour when params are omitted |
| `entities/<slice>/index.ts` | every re-export | one-line description per export |
| `entities/<slice>/model/types.ts` | re-exported types | one-line description + FSD layer reasoning |
| `entities/<slice>/ui/*.tsx` | props interface AND the component | each prop: meaning and accepted values; component: rendering behaviour, colour/format rules |
| `features/<slice>/api/*.action.ts` | every Server Action AND its result union type | HTTP method + endpoint, validation source, `revalidatePath` calls, `@param`, `@returns` |
| `features/<slice>/model/schema.ts` | the zod schema | rule per field (regex, min/max, refinements) |
| `features/<slice>/ui/*.tsx` | props interface AND component | edit/create modes, `defaultValues` shape, success/error UX |
| `features/<slice>/index.ts` | every re-export | one-line description |
| `views/<slice>/ui/*.tsx` | Server Components | which endpoints are fetched in parallel, fallback values, page composition |
| `widgets/<slice>/ui/*.tsx` | props interface AND component | input format (e.g. "decimal string"), formatting rules (locale, currency), adaptive styling |

Reference: `shared/api/dto.ts`, `shared/api/endpoints.ts`, `features/transaction-form/`, `views/transactions/`, `widgets/transactions-kpi/`, `entities/transaction/`.

### Style guide
- One-sentence summary on the first line. Add a blank line before details.
- Use `@param`, `@returns`, `@example`, `@see` from JSDoc — TypeScript types make `@type` redundant.
- Mirror Java field formats EXACTLY: monetary values are decimal strings with scale 4; dates are ISO-8601 UTC instants; IDs are UUID strings. Reference the source Java record by name when re-declaring a DTO.
- For union result types (e.g. `{ ok: true } | { ok: false; message: string }`), describe each branch.
- For Client Components: state `'use client'` context explicitly when behaviour depends on it (e.g. uses `useRouter`).

### Verification before commit
- `npm run typecheck` — no errors.
- `npm run build` — passes.
- IDE hover on `createTransactionAction`, `TransactionResponse`, `<Amount>` MUST show the TSDoc.

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
