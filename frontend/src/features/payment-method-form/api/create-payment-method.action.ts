'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import { paymentMethodSchema, type PaymentMethodFormData } from '../model/schema'

/**
 * Server Action — creates a new payment method for the authenticated user.
 *
 * Validates the payload against {@link paymentMethodSchema}, then calls
 * `POST /api/v1/payment-methods`. Revalidates `/payment-methods` and
 * `/transactions` on success (the transaction form lists payment methods).
 *
 * @param data - Validated payment method form data.
 * @returns `{}` on success or `{ error: string }` on validation/server error.
 */
export async function createPaymentMethodAction(
  data: PaymentMethodFormData,
): Promise<{ error?: string }> {
  const parsed = paymentMethodSchema.safeParse(data)
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Ошибка валидации' }
  }

  const res = await backendFetch(API.paymentMethods.base, {
    method: 'POST',
    body: JSON.stringify(parsed.data),
    forwardAccessToken: true,
  })

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    return { error: problem.detail ?? problem.title ?? 'Ошибка создания метода оплаты' }
  }

  revalidatePath('/payment-methods')
  revalidatePath('/transactions')
  return {}
}
