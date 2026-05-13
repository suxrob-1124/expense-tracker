# CLAUDE.md

## Project Overview
Expense Tracker — full-stack web app for managing personal finances.
- **Backend**: Java 21 / Spring Boot 3.4 → `backend/CLAUDE.md`
- **Frontend**: Next.js 15 / React 19 / FSD → `frontend/CLAUDE.md`
- **DB**: PostgreSQL 16 (local: `postgres/postgres`, db: `expense_tracker`)

## Commands
```bash
docker compose up -d postgres           # DB only (local dev)
docker compose up -d --build            # postgres + backend + frontend (containerised)
docker compose logs -f backend
docker compose logs -f frontend

./scripts/smoke-test.sh                 # E2E; backend must run on :8080
```

## Implementation Status (Steps 1–9 complete, smoke-test 35/35)
Modules: DB, Security, User (CQRS), Auth, Category, Transaction — done.
Frontend routes: `/` → `/transactions`, `/categories`, `/profile`.

<when_committing>
IMPORTANT: Use Conventional Commits: `<type>(<scope>): <subject>`
- Types: `feat` | `fix` | `refactor` | `perf` | `test` | `docs` | `chore` | `style`
- Scopes: `auth` | `user` | `category` | `transactions` | `frontend` | `db` | `security`
- Subject ≤ 72 chars, imperative mood, no period at end
- Pre-commit: `./gradlew build -x test` + `npm run build && npm run lint && npm run typecheck` + `./scripts/smoke-test.sh`
</when_committing>

<when_branching_or_opening_pr>
- Branch from `main`. Naming: `feat/<slug>` | `fix/<slug>` | `refactor/<slug>` | `docs/<slug>` | `chore/<slug>`
- PRs via squash merge only. No direct commits to `main`.
- PR checklist: `./gradlew build -x test` ✓, `npm run build` ✓, `./scripts/smoke-test.sh` ✓, Liquibase `<rollback>` ✓
</when_branching_or_opening_pr>

## AI Rules
- Never bypass security checks or use `--no-verify`. Fix root causes.
- IMPORTANT: Warn immediately on architecture violations (e.g. Auth importing User services).
- No `any` in TypeScript. No raw `Object` in Java.
- Do NOT infer implementation phase from this file — check `git log`.
- `.gitkeep`: keep in empty dirs; remove immediately when code is added.
