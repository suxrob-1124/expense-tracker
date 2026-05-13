'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'

/**
 * Server Action — deletes a payment method by UUID.
 *
 * Calls `DELETE /api/v1/payment-methods/:id`. Linked transactions are
 * preserved by the database FK (`ON DELETE SET NULL`); their `paymentMethodId`
 * becomes null. Revalidates `/payment-methods` and `/transactions` on success.
 *
 * @param id - UUID of the payment method to delete.
 * @returns `{}` on success or `{ error: string }` on failure.
 */
export async function deletePaymentMethodAction(id: string): Promise<{ error?: string }> {
  const res = await backendFetch(API.paymentMethods.byId(id), {
    method: 'DELETE',
    forwardAccessToken: true,
  })

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    return { error: problem.detail ?? problem.title ?? 'Ошибка удаления метода оплаты' }
  }

  revalidatePath('/payment-methods')
  revalidatePath('/transactions')
  return {}
}
