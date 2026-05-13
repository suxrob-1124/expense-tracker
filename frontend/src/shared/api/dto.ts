// Mirrors Java records 1:1
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string    // @Size(min=12, max=128)
  firstName: string   // @Size(max=100)
  lastName: string    // @Size(max=100)
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string // @Size(min=12, max=128)
}

export interface UserResponse {
  id: string          // UUID
  email: string
  firstName: string
  lastName: string
  role: string        // 'USER' | 'ADMIN'
  createdAt: string   // Instant ISO-8601
}

export interface AuthResponse {
  accessToken: string
  tokenType: string   // 'Bearer'
  expiresInSeconds: number
}

export interface CategoryResponse {
  id: string
  name: string
  color: string
  icon: string
}

export type PagedResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
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
