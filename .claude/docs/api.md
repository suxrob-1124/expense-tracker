# API Reference

REST API exposed by the Spring Boot backend.

- **Base URL**: `http://localhost:8080/api/v1`
- **Interactive docs**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Content type**: `application/json` (success) / `application/problem+json` (errors, RFC 7807)
- **Authentication**: `Authorization: Bearer <accessToken>` on every protected endpoint
- **Rate limiting**: `/auth/**` → 10 RPM per IP; everything else → 60 RPM per IP. Excess → `429 Too Many Requests`.

---

## Conventions

### Authentication
- Access token is a 15-minute JWT (HS256). Sent in the `Authorization` header.
- Refresh token is a 7-day token stored in an `HttpOnly; Secure; SameSite=Strict` cookie, scoped to `Path=/api/v1/auth`.
- Every refresh/logout call verifies the token JTI against `revoked_token_jtis`.

### Formats
- `UUID` — RFC 4122, e.g. `3fa85f64-5717-4562-b3fc-2c963f66afa6`.
- `Instant` — UTC ISO-8601, e.g. `2026-05-13T10:00:00Z`.
- `BigDecimal` amounts — decimal **string**, scale 4, e.g. `"99.9900"`.

### Pagination
Endpoints that return lists with `?page=&size=` follow:
- `page` ≥ 0 (default 0)
- `1 ≤ size ≤ 50` (default 20)
- Response wraps the page in `PagedResponse<T>` (see "Common DTOs").

### Errors (RFC 7807)
```json
{
  "type": "https://example.com/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "amount: must be positive",
  "instance": "/api/v1/transactions"
}
```
Common statuses: `400` validation, `401` missing/invalid token, `403` access denied, `404` not found, `409` conflict / optimistic lock / unique violation, `429` rate-limited.

---

## Authentication — `/api/v1/auth` (public)

### `POST /auth/login`
Authenticate user, issue access + refresh tokens.

| Codes | Body |
|---|---|
| `200 OK` | `AuthResponse` + `Set-Cookie: refreshToken=...` |
| `401 Unauthorized` | Wrong credentials, locked account, or disabled user |
| `429 Too Many Requests` | Rate limited |

**Request** — `LoginRequest`
```json
{ "email": "alice@example.com", "password": "S3cret!password" }
```

### `POST /auth/refresh`
Refresh access token using the `refreshToken` cookie.

| Codes | Body |
|---|---|
| `200 OK` | New `AuthResponse` |
| `401 Unauthorized` | Cookie missing/expired/revoked |

### `POST /auth/logout`
Revoke the refresh token (adds JTI to blacklist) and clear cookies.

| Codes | Body |
|---|---|
| `204 No Content` | — |

---

## Users — `/api/v1/users`

### `POST /users/register` *(public)*
Register a new account.

| Codes | Body |
|---|---|
| `201 Created` | `UserResponse` |
| `400 Bad Request` | Validation failed |
| `409 Conflict` | Email already registered |

**Request** — `RegisterRequest`
```json
{
  "email": "alice@example.com",
  "password": "S3cret!password",
  "firstName": "Alice",
  "lastName": "Doe"
}
```

### `GET /users/me` *(bearer)*
Get current user profile.

| Codes | Body |
|---|---|
| `200 OK` | `UserResponse` |
| `401 Unauthorized` | — |

### `POST /users/me/password` *(bearer)*
Change password.

| Codes | Body |
|---|---|
| `204 No Content` | — |
| `400 Bad Request` | Validation failed |
| `401 Unauthorized` | Old password incorrect |

**Request** — `ChangePasswordRequest`
```json
{ "currentPassword": "oldPass!", "newPassword": "newPass2!" }
```

---

## Categories — `/api/v1/categories` *(bearer)*

User-owned categories. Name must be unique per user (case-insensitive).

### `POST /categories`
Create a category.

| Codes | Body |
|---|---|
| `201 Created` | `CategoryResponse` |
| `400 Bad Request` | Validation failed |
| `409 Conflict` | Duplicate name for this user |

**Request** — `CategoryRequest`
```json
{ "name": "Groceries", "color": "#22c55e", "icon": "shopping-cart" }
```

### `GET /categories`
List all categories owned by the authenticated user, sorted by name.

| Codes | Body |
|---|---|
| `200 OK` | `CategoryResponse[]` |

### `GET /categories/{id}`
Get a single category.

| Codes | Body |
|---|---|
| `200 OK` | `CategoryResponse` |
| `404 Not Found` | Not found or owned by another user |

### `PUT /categories/{id}`
Replace category data.

| Codes | Body |
|---|---|
| `200 OK` | `CategoryResponse` |
| `400 Bad Request` | Validation failed |
| `404 Not Found` | — |
| `409 Conflict` | Duplicate name |

### `DELETE /categories/{id}`
Delete a category. Cascades to transactions (FK `ON DELETE CASCADE`).

| Codes | Body |
|---|---|
| `204 No Content` | — |
| `404 Not Found` | — |

---

## Transactions — `/api/v1/transactions` *(bearer)*

### `POST /transactions`
Create a transaction.

| Codes | Body |
|---|---|
| `201 Created` | `TransactionResponse` |
| `400 Bad Request` | Validation failed |
| `404 Not Found` | Referenced category does not exist or is owned by another user |

**Request** — `TransactionRequest`
```json
{
  "amount": "99.99",
  "type": "EXPENSE",
  "description": "Lunch",
  "date": "2026-05-13T10:00:00Z",
  "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```
- `amount` — decimal string, scale 4, > 0.
- `type` — `INCOME` or `EXPENSE`.
- `description` — optional, ≤ 255 chars.

### `GET /transactions/latest?page=&size=`
List latest transactions with pagination, sorted by `date DESC`.

| Codes | Body |
|---|---|
| `200 OK` | `PagedResponse<TransactionResponse>` |
| `400 Bad Request` | `page` or `size` out of range |

### `GET /transactions?year=&month=`
List all transactions for a calendar month (UTC). Defaults to the current UTC month when both params are omitted.

| Codes | Body |
|---|---|
| `200 OK` | `TransactionResponse[]` |
| `400 Bad Request` | Invalid `year`/`month` |

### `GET /transactions/summary?year=&month=`
Monthly financial summary: aggregated income, expenses, balance. Defaults to current UTC month.

| Codes | Body |
|---|---|
| `200 OK` | `TransactionSummaryResponse` |

**Response** — `TransactionSummaryResponse`
```json
{
  "year": 2026,
  "month": 5,
  "totalIncome": "12500.0000",
  "totalExpense": "8430.1500",
  "balance": "4069.8500"
}
```

### `GET /transactions/{id}`
Get a single transaction.

| Codes | Body |
|---|---|
| `200 OK` | `TransactionResponse` |
| `404 Not Found` | Not found or owned by another user |

### `PATCH /transactions/{id}`
Partially update a transaction. Only non-null fields are applied.

| Codes | Body |
|---|---|
| `200 OK` | `TransactionResponse` |
| `400 Bad Request` | Validation failed |
| `404 Not Found` | — |
| `409 Conflict` | Optimistic lock failure |

**Request** — `TransactionPatchRequest`
```json
{ "amount": "49.99", "description": "Lunch (corrected)" }
```

### `DELETE /transactions/{id}`
Delete a transaction.

| Codes | Body |
|---|---|
| `204 No Content` | — |
| `404 Not Found` | — |

---

## Common DTOs

### `AuthResponse`
```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### `UserResponse`
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Doe",
  "role": "ROLE_USER",
  "createdAt": "2026-05-13T10:00:00Z"
}
```

### `CategoryResponse`
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Groceries",
  "color": "#22c55e",
  "icon": "shopping-cart"
}
```

### `TransactionResponse`
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": "99.9900",
  "type": "EXPENSE",
  "description": "Lunch",
  "date": "2026-05-13T10:00:00Z",
  "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "createdAt": "2026-05-13T10:00:00Z",
  "updatedAt": "2026-05-13T10:00:00Z"
}
```

### `PagedResponse<T>`
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7
}
```

---

## Open / unprotected endpoints

| Path | Notes |
|---|---|
| `POST /api/v1/auth/login` | Rate-limited |
| `POST /api/v1/auth/refresh` | Rate-limited |
| `POST /api/v1/auth/logout` | Rate-limited |
| `POST /api/v1/users/register` | Rate-limited |
| `GET /actuator/health` | Liveness probe |
| `GET /v3/api-docs/**` | OpenAPI spec |
| `GET /swagger-ui/**` | Swagger UI |
