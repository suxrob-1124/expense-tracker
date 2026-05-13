# Database Schema

PostgreSQL 16. Schema is managed exclusively by Liquibase — Hibernate runs with `ddl-auto: validate`.

- **Master changelog**: `backend/src/main/resources/db/changelog/db.changelog-master.xml`
- **Changesets directory**: `backend/src/main/resources/db/changelog/changes/`
- **Naming convention**: `YYYYMMDD-NNN-description.xml`
- **Every changeset MUST include a `<rollback>` block.**

## Entity-Relationship Overview

```
                 ┌───────────────────────┐
                 │        users          │
                 │  id (PK, UUID)        │
                 │  email_hash (UQ)      │
                 └──┬───────────┬────────┘
        FK CASCADE  │           │  FK CASCADE
                    │           │
        ┌───────────▼──┐     ┌──▼─────────────────────┐
        │ categories   │     │     transactions       │
        │ id (PK)      │     │  id (PK)               │
        │ user_id (FK) │◄────┤  category_id (FK)      │
        │ UQ (user_id, │     │  user_id (FK)          │
        │   LOWER(name))│    │                        │
        └──────────────┘     └────────────────────────┘

   ┌─────────────────┐        ┌──────────────────────┐
   │  audit_events   │        │  revoked_token_jtis  │
   │  id (PK)        │        │  jti (PK, VARCHAR36) │
   │  user_id        │        │  expires_at          │
   │  payload (jsonb)│        └──────────────────────┘
   └─────────────────┘
   (no FK — audit log
    must survive user
    deletions)
```

Audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`) live on every business table via `BaseEntity`. They are filled automatically by `SpringSecurityAuditorAware`. `version` (BIGINT, `@Version`) provides optimistic locking on every entity.

---

## `users` *(changeset `20260506-001`)*

Stores the user account. PII is encrypted at rest with AES-256-GCM; lookup is performed via the deterministic `email_hash` column.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `UUID` | PK | Primary identifier |
| `email_encrypted` | `TEXT` | NOT NULL | Ciphertext of the email — `Base64(IV‖ciphertext‖tag)` produced by `AesGcmStringConverter`. Never queried directly. |
| `email_hash` | `VARCHAR(64)` | NOT NULL, UNIQUE, indexed (`idx_users_email_hash`) | `SHA-256(lower(trim(email)))` — used for uniqueness checks and login lookup. |
| `password_hash` | `VARCHAR(72)` | NOT NULL | BCrypt hash, strength=12 (fixed 60-char output, 72 leaves headroom). |
| `first_name_encrypted` | `TEXT` | nullable | AES-256-GCM ciphertext. |
| `last_name_encrypted` | `TEXT` | nullable | AES-256-GCM ciphertext. |
| `role` | `VARCHAR(32)` | NOT NULL, default `ROLE_USER` | Enum: `ROLE_USER` \| `ROLE_ADMIN`. |
| `enabled` | `BOOLEAN` | NOT NULL, default `true` | Soft-disable flag — disabled users cannot log in. |
| `failed_login_attempts` | `INT` | NOT NULL, default `0` | Brute-force counter. Reset on successful login. |
| `locked_until` | `TIMESTAMP` | nullable | If in the future, login is refused regardless of credentials. |
| `last_login_at` | `TIMESTAMP` | nullable | Set on every successful login by `UserLoginActivityListener`. |
| `version` | `BIGINT` | NOT NULL | Optimistic lock counter (`@Version`). |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | NOT NULL | Audit columns (`BaseEntity`). |

**Indexes**: `idx_users_email_hash` (UNIQUE) on `email_hash`.

---

## `categories` *(changeset `20260506-003`)*

User-owned expense/income categories.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `UUID` | PK | Primary identifier |
| `name` | `VARCHAR(64)` | NOT NULL | Display name. Must be unique per user (case-insensitive — see `idx_categories_user_name_lower`). |
| `color` | `VARCHAR(7)` | NOT NULL | Hex color, e.g. `#22c55e`. Used in UI for the category card and amount accent. |
| `icon` | `VARCHAR(32)` | NOT NULL | `lucide-react` icon name, e.g. `shopping-cart`. |
| `user_id` | `UUID` | NOT NULL, FK → `users(id)` `ON DELETE CASCADE` | Owner. |
| `version`, audit columns | — | NOT NULL | Optimistic lock + auditing. |

**Indexes**:
- `idx_categories_user_id` on `user_id` — fast ownership scans.
- `idx_categories_user_name_lower` (UNIQUE, raw SQL) on `(user_id, LOWER(name))` — case-insensitive name uniqueness per user.

---

## `transactions` *(changeset `20260507-004`)*

Financial transactions belonging to a user, classified by category.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `UUID` | PK | Primary identifier |
| `amount` | `DECIMAL(19,4)` | NOT NULL | Monetary amount. Always positive — direction is conveyed by `type`. Scale 4 to support sub-cent precision. |
| `type` | `VARCHAR(16)` | NOT NULL | Enum: `INCOME` \| `EXPENSE`. |
| `description` | `VARCHAR(255)` | nullable | Free-form note. |
| `date` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | The instant the transaction occurred (user-supplied, UTC). Used for month bucketing and ordering. |
| `category_id` | `UUID` | NOT NULL, FK → `categories(id)` `ON DELETE CASCADE` | Classification. Must be owned by the same user as the transaction. |
| `user_id` | `UUID` | NOT NULL, FK → `users(id)` `ON DELETE CASCADE` | Owner. |
| `version`, audit columns | — | NOT NULL | Optimistic lock + auditing. |

**Indexes**:
- `idx_transactions_user_id` on `user_id` — ownership filter.
- `idx_transactions_category_id` on `category_id` — required for FK + delete cascade.
- `idx_transactions_date` on `date` — monthly range scans.
- `idx_transactions_user_date` on `(user_id, date DESC)` — composite, powers `/transactions/latest` (paginated, newest first).

---

## `audit_events` *(changeset `20260506-002`)*

Append-only audit log. **No FK to `users`** so records survive user deletion.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `UUID` | PK | Event identifier |
| `version` | `BIGINT` | NOT NULL | Optimistic lock |
| `event_type` | `VARCHAR(64)` | NOT NULL | E.g. `USER_REGISTERED`, `USER_LOGGED_IN`, `USER_LOGIN_FAILED`, `PASSWORD_CHANGED`. |
| `user_id` | `UUID` | nullable | The subject. Nullable so failed logins for non-existent users can still be logged. |
| `payload` | `jsonb` | nullable | Free-form structured context — e.g. `{ "reason": "BAD_CREDENTIALS" }`. |
| `occurred_at` | `TIMESTAMP` | NOT NULL, indexed | Event timestamp |
| `ip_address` | `VARCHAR(45)` | nullable | Source IP (IPv4 or IPv6). |

**Indexes**: `idx_audit_events_user_id`, `idx_audit_events_occurred_at`.

---

## `revoked_token_jtis` *(changeset `20260507-005`, version added `20260513-006`)*

JWT revocation list. Looked up on every `/auth/refresh` and `/auth/logout` call.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `jti` | `VARCHAR(36)` | PK | The JWT ID (`jti` claim). |
| `expires_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL, indexed | Token's original expiry. Rows past this point can be safely garbage-collected. |
| `version` | `BIGINT` | NOT NULL | Added in `20260513-006` for optimistic locking. |

**Indexes**: `idx_revoked_token_jtis_expires_at` — supports housekeeping (`DELETE WHERE expires_at < now()`).

---

## Changeset history

| ID | Description |
|---|---|
| `20260506-001` | Create `users` table + `idx_users_email_hash` |
| `20260506-002` | Create `audit_events` table + indexes |
| `20260506-003` | Create `categories` table + FK to `users` + uniqueness on `(user_id, LOWER(name))` |
| `20260507-004` | Create `transactions` table + FKs + indexes |
| `20260507-005` | Create `revoked_token_jtis` table + `expires_at` index |
| `20260513-006` | Add `version` column to `revoked_token_jtis` |

---

## Operational notes

- **`ddl-auto: validate`** — Spring Boot will fail to start if the JPA model does not match the schema. Always create a migration first, then the entity.
- **Cascading deletes**: deleting a `User` removes their `categories` and `transactions`. Audit events are preserved (no FK).
- **Encryption rotation**: rotating `FIELD_ENCRYPTION_KEY` invalidates all encrypted columns — a re-encryption job is required (not yet implemented).
- **Time zones**: `transactions.date` is `TIMESTAMP WITH TIME ZONE`; everything else uses `TIMESTAMP` and stores UTC by convention (Spring serializes `Instant` accordingly).
