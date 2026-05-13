/**
 * Re-exports of transaction-related DTOs from the shared API layer.
 *
 * Import these types from `@/entities/transaction` instead of reaching into
 * `@/shared/api/dto` directly, to respect FSD layer boundaries.
 */
export type { TransactionType, TransactionResponse, TransactionRequest } from '@/shared/api/dto'
