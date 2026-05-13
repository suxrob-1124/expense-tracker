---
name: docs-frontend
description: Use this skill when creating or modifying any public export from a Next.js / FSD slice in the frontend (Server Actions, components, props interfaces, zod schemas, shared DTOs, endpoint helpers). Trigger keywords — touching `features/`, `entities/`, `widgets/`, `views/`, `shared/api/`, `index.ts` barrel exports, adding Server Action, adding a component, adding a zod schema, adding an API endpoint helper.
model: claude-sonnet-4-6
effort: medium
allowed-tools: Read, Edit, Write, Grep, Glob, Bash(npm run build*), Bash(npm run lint*), Bash(npm run typecheck*)
---

# Frontend documentation rules (TSDoc)

Apply these rules to every NEW or MODIFIED public export from a slice. **No exceptions.** Documentation ships in the same commit as the code.

## Language
ALL TSDoc/JSDoc MUST be in **English**, regardless of the chat language.

## What requires TSDoc (per FSD layer)

| Layer | What to document | Focus on |
|---|---|---|
| `shared/api/dto.ts` | every interface/type mirroring a Java DTO | which Java record it mirrors, format of every field (e.g. "ISO-8601 UTC instant", "decimal string, scale 4"), realistic example |
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

## Style guide

- One-sentence summary on the first line. Add a blank line before details.
- Use `@param`, `@returns`, `@example`, `@see` — TypeScript types make `@type` redundant.
- Mirror Java field formats EXACTLY: monetary values are decimal strings with scale 4; dates are ISO-8601 UTC instants; IDs are UUID strings. Reference the source Java record by name when re-declaring a DTO.
- For union result types (e.g. `{ ok: true } | { ok: false; message: string }`), describe each branch.
- For Client Components: state `'use client'` context explicitly when behaviour depends on it (e.g. uses `useRouter`).

## Reference implementation (mimic these)
- `frontend/src/shared/api/dto.ts`
- `frontend/src/shared/api/endpoints.ts`
- `frontend/src/features/transaction-form/` (action, schema, ui, index)
- `frontend/src/views/transactions/ui/TransactionsView.tsx`
- `frontend/src/widgets/transactions-kpi/ui/TransactionsKpi.tsx`
- `frontend/src/entities/transaction/`

## Verification before commit
1. `npm run typecheck` — no errors.
2. `npm run build` — passes.
3. IDE hover on a new action / type / component MUST show the TSDoc.
