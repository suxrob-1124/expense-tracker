# Code Review Guide

## Checklist

### General
- [ ] PR title follows Conventional Commits: `<type>(<scope>): <subject>` ≤ 72 chars
- [ ] `./gradlew build -x test` passes
- [ ] `npm run build && npm run lint && npm run typecheck` passes
- [ ] `./scripts/smoke-test.sh` — 35/35

### Backend
- [ ] `Controller` — orchestration only, no business logic
- [ ] `@PreAuthorize` is in `Service`, not `Controller`
- [ ] `Auth` module does not import `UserRepository` or user services directly
- [ ] `*CommandService` — `@Transactional`, `*QueryService` — `readOnly = true`
- [ ] All DTOs are Java `record`s; mapping is MapStruct only
- [ ] Monetary values use `BigDecimal` scale 4 — no `float`/`double`
- [ ] Every entity has `@Version Long version`
- [ ] PII fields (`email`, `firstName`, `lastName`) stored encrypted via `AesGcmStringConverter`
- [ ] Email lookup uses `email_hash`, never `email_encrypted`
- [ ] Errors follow RFC 7807 (`application/problem+json`)
- [ ] New Liquibase changeset includes `<rollback>`

### Frontend
- [ ] FSD imports flow top-down only: `app → views → widgets → features → entities → shared`
- [ ] Each slice exports only through its `index.ts`
- [ ] Data fetching in Server Components — no `useEffect` for data loading
- [ ] Mutations use Server Actions (`'use server'`) — no API route handlers
- [ ] All backend calls go through `backendFetch` from `shared/api/http.ts`
- [ ] No `any` in TypeScript
- [ ] Forms: `react-hook-form` + zod v4, uses `.issues` not `.errors`
- [ ] Errors shown via `sonner` (`toast.error`)

---

## Style & Naming

### Backend
| Category | Pattern | Example |
|---|---|---|
| Controller | `EntityController` | `CategoryController` |
| Command service | `EntityCommandService` | `CategoryCommandService` |
| Query service | `EntityQueryService` | `CategoryQueryService` |
| Mapper | `EntityMapper` | `CategoryMapper` |
| Request DTO | `EntityRequest` | `CategoryRequest` |
| Patch DTO | `EntityPatchRequest` | `TransactionPatchRequest` |
| Response DTO | `EntityResponse` | `CategoryResponse` |
| Event | `EntityVerbedEvent` | `UserRegisteredEvent` |
| Changeset | `YYYYMMDD-NNN-description.xml` | `20260508-006-add-tags.xml` |

### Frontend
| Category | Pattern | Example |
|---|---|---|
| Server Action file | `entity.action.ts` | `transaction.action.ts` |
| Form component | `EntityCreateForm` / `EntityForm` | `CategoryCreateForm` |
| Zod schema | `entitySchema` | `transactionSchema` |
| API constant | named export in `endpoints.ts` | `TRANSACTIONS_URL` |
| Entity type | `Entity` in `entity/model/types.ts` | `Transaction` |

---

## Skip During Review
- Already-released Liquibase changesets (dates before this PR)
- `package-lock.json`
- `*.log`, `*.tmp`
- `build/`, `.next/`, `out/`
- `.gitkeep`
