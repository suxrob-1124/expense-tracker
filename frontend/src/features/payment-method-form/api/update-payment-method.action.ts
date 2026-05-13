'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { PaymentMethodPatchRequest } from '@/shared/api/dto'

/**
 * Server Action — partially updates a payment method.
 *
 * Calls `PATCH /api/v1/payment-methods/:id`. Use this to rename, change type,
 * adjust balance, or toggle the {@link PaymentMethodPatchRequest.archived} flag —
 * there is no dedicated archive endpoint.
 *
 * @param id    - UUID of the payment method to update.
 * @param patch - Fields to apply; null/undefined fields are ignored by the backend.
 * @returns `{}` on success or `{ error: string }` on failure.
 */
export async function updatePaymentMethodAction(
  id: string,
  patch: PaymentMethodPatchRequest,
): Promise<{ error?: string }> {
  const res = await backendFetch(API.paymentMethods.byId(id), {
    method: 'PATCH',
    body: JSON.stringify(patch),
    forwardAccessToken: true,
  })

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    return { error: problem.detail ?? problem.title ?? 'Ошибка обновления метода оплаты' }
  }

  revalidatePath('/payment-methods')
  revalidatePath('/transactions')
  return {}
}
