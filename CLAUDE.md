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

<when_committing_or_branching_or_opening_pr>
Run `/commit-and-pr` for commit format, branch naming, pre-commit checks, and PR checklist.
</when_committing_or_branching_or_opening_pr>

## AI Rules
- Never bypass security checks or use `--no-verify`. Fix root causes.
- IMPORTANT: Warn immediately on architecture violations (e.g. Auth importing User services).
- No `any` in TypeScript. No raw `Object` in Java.
- Do NOT infer implementation phase from this file — check `git log`.
- `.gitkeep`: keep in empty dirs; remove immediately when code is added.

## Documentation Maintenance — keep `.claude/docs/*` in sync
Run `/docs-sync` before committing any non-trivial change to check which `.claude/docs/*` files need updating.

## Documentation — write it alongside the code (NOT later)
- **Language**: ALL documentation (JavaDoc, TSDoc, OpenAPI `description`/`summary`) MUST be written in **English** — even when chatting with the user in Russian.
- **Backend**: run `/docs-backend` when creating/modifying controller, service, repository, mapper, or DTO.
- **Frontend**: run `/docs-frontend` when creating/modifying a public export from any FSD slice.
- **Reference implementation**: the `transactions` module (PR #3) is the canonical example for both layers — mimic its structure and tone.
