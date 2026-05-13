---
name: commit-and-pr
description: Use this skill when committing, creating a branch, or opening a pull request. Enforces Conventional Commits format, scope list, pre-commit checks, and PR rules. Trigger keywords — "commit", "branch", "pull request", "PR", "merge", "push", "squash".
model: claude-sonnet-4-6
effort: low
allowed-tools: Read, Grep, Bash(git:*), Bash(gh:*), Bash(./gradlew build*), Bash(npm run build*), Bash(npm run lint*), Bash(npm run typecheck*), Bash(./scripts/smoke-test.sh)
---

# Commit, branch, and PR rules

## Commit message format — Conventional Commits

```
<type>(<scope>): <subject>
```

- **Types**: `feat` | `fix` | `refactor` | `perf` | `test` | `docs` | `chore` | `style`
- **Scopes**: `auth` | `user` | `category` | `transactions` | `frontend` | `db` | `security`
- **Subject**: ≤ 72 chars, imperative mood, no period at end

## Pre-commit checks — ALL must pass before committing

```bash
./gradlew build -x test
npm run build && npm run lint && npm run typecheck
./scripts/smoke-test.sh
```

Never use `--no-verify`. Fix root causes.

## Branching

- Always branch from `main`.
- Naming: `feat/<slug>` | `fix/<slug>` | `refactor/<slug>` | `docs/<slug>` | `chore/<slug>`
- No direct commits to `main`.

## Pull requests

- Squash merge only.
- PR checklist before opening:
  - [ ] `./gradlew build -x test` passes
  - [ ] `npm run build` passes
  - [ ] `./scripts/smoke-test.sh` passes
  - [ ] Every Liquibase changeset has a `<rollback>` block
