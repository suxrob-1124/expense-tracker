/** Payment method creation form Client Component. */
export { PaymentMethodForm } from './ui/PaymentMethodForm'
/** Server Action — creates a new payment method. */
export { createPaymentMethodAction } from './api/create-payment-method.action'
/** Server Action — partially updates a payment method (use `archived` to toggle). */
export { updatePaymentMethodAction } from './api/update-payment-method.action'
/** Server Action — flips the archived flag of a payment method (id, archived). */
export { toggleArchivePaymentMethodAction } from './api/toggle-archive-payment-method.action'
/** Server Action — deletes a payment method (linked transactions keep history). */
export { deletePaymentMethodAction } from './api/delete-payment-method.action'
/** Zod schema and inferred form data type. */
export { paymentMethodSchema, type PaymentMethodFormData } from './model/schema'
