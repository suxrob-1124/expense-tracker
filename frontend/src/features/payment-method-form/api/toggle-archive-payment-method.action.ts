'use server'

import { updatePaymentMethodAction } from './update-payment-method.action'

/**
 * Server Action — toggles the archive flag of a payment method.
 *
 * Thin wrapper around {@link updatePaymentMethodAction} with a `(id, archived)`
 * signature suitable for being passed as a prop from a Server Component to a
 * Client Component (which cannot otherwise build a closure over the patch body).
 *
 * @param id       - UUID of the payment method.
 * @param archived - New archive state (true to hide, false to restore).
 */
export async function toggleArchivePaymentMethodAction(
  id: string,
  archived: boolean,
): Promise<{ error?: string }> {
  return updatePaymentMethodAction(id, { archived })
}
