---
name: docs-sync
description: Use this skill when finishing any non-trivial change to check which .claude/docs/* files need updating. Trigger keywords — "done", "finished", "commit", "PR", after changing an endpoint, DTO, controller, status code, Liquibase changeset, column, index, FK, module, layer, or dev-guide recipe.
model: claude-haiku-4-5-20251001
effort: low
allowed-tools: Read, Edit, Grep, Glob
---

# .claude/docs/* sync checklist

Before committing, verify each doc below against what you changed. A PR that leaves these stale is incomplete.

| What changed | Doc to update |
|---|---|
| Endpoint, DTO, controller, status code | `.claude/docs/api.md` |
| Liquibase changeset, column, index, FK | `.claude/docs/database.md` |
| New module, layer, pattern, cross-module dependency | `.claude/docs/architecture.md` |
| Recipe for adding a module / feature / migration | `.claude/docs/dev-guide.md` |

## Before starting any non-trivial change
Consult all four docs first:
- `.claude/docs/architecture.md`
- `.claude/docs/api.md`
- `.claude/docs/database.md`
- `.claude/docs/dev-guide.md`
