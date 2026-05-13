---
name: liquibase-changeset
description: Use this skill when adding or modifying a Liquibase changeset — new column, table, index, foreign key, or data migration. Trigger keywords — "migration", "changeset", "Liquibase", "schema change", "add column", "new table", "index", "FK".
model: claude-sonnet-4-6
effort: medium
allowed-tools: Read, Edit, Write, Grep, Glob, Bash(./gradlew bootRun*), Bash(./gradlew build*), Bash(ls:*)
---

# Liquibase changeset rules

## Non-negotiable
- **Never** set `ddl-auto: create` or `ddl-auto: update` — only `validate`. Liquibase owns the schema.
- Every changeset MUST include a `<rollback>` block. A PR without rollback is incomplete.

## File location and naming

```
src/main/resources/db/changelog/changes/YYYYMMDD-NNN-description.xml
```

- `YYYYMMDD` — today's date.
- `NNN` — next sequential number (3 digits, zero-padded).
- `description` — short kebab-case summary of what the changeset does.

## Existing changesets (do not modify)

| ID | Description |
|---|---|
| `20260506-001` | users table |
| `20260506-002` | audit_events table |
| `20260506-003` | categories table |
| `20260507-004` | transactions table |
| `20260507-005` | revoked_token_jtis table |

Pick the next number after `005` for new changesets.

## After adding a changeset
Update `.claude/docs/database.md` to reflect the new table/column/index — a PR that changes the schema but leaves the doc stale is incomplete.
