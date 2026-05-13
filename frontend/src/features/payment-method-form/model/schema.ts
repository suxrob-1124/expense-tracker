import { z } from 'zod'

/**
 * Zod schema for the payment method form.
 *
 * - `name`: 1–64 chars, required.
 * - `type`: enum CARD | CASH | BANK.
 * - `last4`: optional; when present, exactly 4 digits.
 * - `balance`: optional; decimal string with up to 4 fractional digits, non-negative.
 *
 * Empty strings for `last4` and `balance` are coerced to `undefined` so the
 * server receives a clean payload (the backend's MapStruct skips undefined fields).
 */
export const paymentMethodSchema = z.object({
  name: z.string().min(1, 'Название обязательно').max(64, 'Не более 64 символов'),
  type: z.enum(['CARD', 'CASH', 'BANK']),
  last4: z
    .union([z.string().regex(/^\d{4}$/, 'Введите ровно 4 цифры'), z.literal('')])
    .optional()
    .transform((v) => (v === '' ? undefined : v)),
  balance: z
    .union([
      z
        .string()
        .regex(/^\d+(\.\d{1,4})?$/, 'Введите корректное число (например: 100 или 100.50)'),
      z.literal(''),
    ])
    .optional()
    .transform((v) => (v === '' ? undefined : v)),
})

/**
 * Input type (pre-transform) — used for `useForm<PaymentMethodFormData>`.
 * `z.input` gives the shape that react-hook-form manages (with optional `last4`/`balance`).
 * The Server Action re-parses with `paymentMethodSchema.safeParse` to get the transformed output.
 */
export type PaymentMethodFormData = z.input<typeof paymentMethodSchema>
