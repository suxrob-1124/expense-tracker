/**
 * Payload for {@link API.auth.login} (`POST /api/v1/auth/login`).
 * Mirrors `com.company.expensetracker.dto.auth.LoginRequest`.
 */
export interface LoginRequest {
  /** User's email address. Example: `"alice@example.com"` */
  email: string
  /** User's plaintext password (sent over HTTPS only). */
  password: string
}

/**
 * Payload for {@link API.users.register} (`POST /api/v1/users/register`).
 * Mirrors `com.company.expensetracker.dto.user.RegisterRequest`.
 *
 * Password: min 12, max 128 characters.
 * firstName / lastName: max 100 characters each.
 */
export interface RegisterRequest {
  /** User's email address. Example: `"alice@example.com"` */
  email: string
  /** Password (12–128 chars). Stored as BCrypt hash server-side. */
  password: string
  /** First name (max 100 chars). Example: `"Alice"` */
  firstName: string
  /** Last name (max 100 chars). Example: `"Smith"` */
  lastName: string
}

/**
 * Payload for `PATCH /api/v1/users/me/password`.
 * Mirrors `com.company.expensetracker.dto.user.ChangePasswordRequest`.
 */
export interface ChangePasswordRequest {
  /** The user's current password, required for verification. */
  currentPassword: string
  /** New password (12–128 chars). */
  newPassword: string
}

/**
 * User profile returned after registration or via `GET /api/v1/users/me`.
 * Mirrors `com.company.expensetracker.dto.user.UserResponse`.
 */
export interface UserResponse {
  /** UUID of the user. Example: `"3fa85f64-5717-4562-b3fc-2c963f66afa6"` */
  id: string
  /** User's email address (stored encrypted on the backend). */
  email: string
  /** First name (stored encrypted on the backend). */
  firstName: string
  /** Last name (stored encrypted on the backend). */
  lastName: string
  /** Assigned role. Example: `"USER"` */
  role: string
  /** Account creation timestamp — ISO-8601 UTC instant. Example: `"2026-05-13T10:00:00Z"` */
  createdAt: string
}

/**
 * Response body from {@link API.auth.login} and {@link API.auth.refresh}.
 * Mirrors `com.company.expensetracker.dto.auth.AuthResponse`.
 *
 * The companion refresh token is delivered as an `HttpOnly` cookie and is absent here.
 */
export interface AuthResponse {
  /** Short-lived JWT access token (Bearer, HS256). Example: `"eyJhbGciOiJIUzI1NiJ9..."` */
  accessToken: string
  /** Token scheme — always `"Bearer"`. */
  tokenType: string
  /** Access token lifetime in seconds. Example: `900` */
  expiresInSeconds: number
}

/**
 * Category as returned by the API.
 * Mirrors `com.company.expensetracker.dto.category.CategoryResponse`.
 */
export interface CategoryResponse {
  /** UUID of the category. Example: `"3fa85f64-5717-4562-b3fc-2c963f66afa6"` */
  id: string
  /** Category display name. Example: `"Groceries"` */
  name: string
  /** Color code associated with the category. Example: `"#4ade80"` */
  color: string
  /** Icon identifier from the predefined icon set. Example: `"shopping-cart"` */
  icon: string
}

/**
 * Generic paginated response wrapper.
 * Mirrors `com.company.expensetracker.dto.common.PagedResponse<T>`.
 */
export type PagedResponse<T> = {
  /** Page items. */
  content: T[]
  /** Zero-based page index. */
  page: number
  /** Number of items per page. */
  size: number
  /** Total number of items across all pages. */
  totalElements: number
  /** Total number of pages. */
  totalPages: number
  /** Whether this is the last page. */
  last: boolean
}

/**
 * Discriminated union for payment method type.
 * Mirrors `com.company.expensetracker.domain.PaymentMethodType`.
 */
export type PaymentMethodType = 'CARD' | 'CASH' | 'BANK'

/**
 * Payload for creating a payment method.
 * Mirrors `com.company.expensetracker.dto.paymentmethod.PaymentMethodRequest`.
 */
export interface PaymentMethodRequest {
  /** Display name (1–64 chars, unique per user, case-insensitive). */
  name: string
  /** Payment method type. Example: `"CARD"` */
  type: PaymentMethodType
  /** Optional last four digits (4 numeric chars). Example: `"1234"` */
  last4?: string | null
  /** Optional initial balance — decimal string scale 4. Example: `"1000.0000"` */
  balance?: string | null
}

/**
 * Partial-update payload for a payment method (PATCH semantics).
 * Mirrors `com.company.expensetracker.dto.paymentmethod.PaymentMethodPatchRequest`.
 *
 * Every field is optional; null fields are ignored by the backend.
 * Use {@link archived} to toggle archive state — there is no dedicated archive endpoint.
 */
export interface PaymentMethodPatchRequest {
  name?: string | null
  type?: PaymentMethodType | null
  last4?: string | null
  balance?: string | null
  archived?: boolean | null
}

/**
 * Payment method returned by the API.
 * Mirrors `com.company.expensetracker.dto.paymentmethod.PaymentMethodResponse`.
 */
export interface PaymentMethodResponse {
  /** UUID of the payment method. Example: `"3fa85f64-5717-4562-b3fc-2c963f66afa6"` */
  id: string
  /** Display name. Example: `"Visa Gold"` */
  name: string
  type: PaymentMethodType
  /** Last four digits or null. Example: `"1234"` */
  last4: string | null
  /** Current balance as decimal string scale 4, or null. Example: `"1000.0000"` */
  balance: string | null
  /** Archive flag — archived methods are hidden from default lists. */
  archived: boolean
  /** ISO-8601 UTC instant. */
  createdAt: string
  /** ISO-8601 UTC instant. */
  updatedAt: string
}

/**
 * Discriminated union for transaction direction.
 * Mirrors `com.company.expensetracker.domain.TransactionType`.
 */
export type TransactionType = 'INCOME' | 'EXPENSE'

/**
 * Payload for creating a transaction.
 * Mirrors `com.company.expensetracker.dto.transaction.TransactionRequest`.
 *
 * `amount` is a decimal string (e.g. `"99.99"`) to avoid floating-point rounding.
 * `date` is an ISO-8601 UTC instant string (e.g. `"2026-05-13T10:00:00Z"`).
 */
export interface TransactionRequest {
  /** Decimal string, must be ≥ 0.01. Example: `"99.99"` */
  amount: string
  type: TransactionType
  description?: string | null
  /** ISO-8601 UTC instant. Example: `"2026-05-13T10:00:00Z"` */
  date: string
  /** UUID of the owning category */
  categoryId: string
  /** Optional UUID of the payment method used (ownership is verified by the backend). */
  paymentMethodId?: string | null
}

/**
 * Transaction returned by the API.
 * Mirrors `com.company.expensetracker.dto.transaction.TransactionResponse`.
 *
 * Monetary values are decimal strings with scale 4 (e.g. `"99.9900"`).
 */
export interface TransactionResponse {
  /** UUID of the transaction */
  id: string
  /** Decimal string with scale 4. Example: `"99.9900"` */
  amount: string
  type: TransactionType
  description: string | null
  /** ISO-8601 UTC instant */
  date: string
  /** UUID of the associated category */
  categoryId: string
  /** UUID of the linked payment method, or null if none. */
  paymentMethodId: string | null
  /** ISO-8601 UTC instant */
  createdAt: string
  /** ISO-8601 UTC instant */
  updatedAt: string
}

/**
 * Monthly income/expense summary.
 * Mirrors `com.company.expensetracker.dto.transaction.TransactionSummaryResponse`.
 *
 * All values are decimal strings with scale 4.
 * `balance` = `income` − `expense`.
 */
export interface TransactionSummaryResponse {
  /** Total income for the period. Example: `"1500.0000"` */
  income: string
  /** Total expenses for the period. Example: `"800.0000"` */
  expense: string
  /** Net balance (income minus expense). Example: `"700.0000"` */
  balance: string
}
