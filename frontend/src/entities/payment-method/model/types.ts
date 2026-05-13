/**
 * Re-exports payment method DTOs from `shared/api/dto` so that downstream
 * slices import them through the `entities/payment-method` barrel.
 *
 * Keeping the type aliases in the entities layer follows FSD rules:
 * features and widgets must not reach into `shared/api/dto` directly when
 * they conceptually deal with the payment method domain.
 */
export type {
  PaymentMethodType,
  PaymentMethodResponse,
  PaymentMethodRequest,
  PaymentMethodPatchRequest,
} from '@/shared/api/dto'
