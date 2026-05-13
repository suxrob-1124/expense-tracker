import { z } from 'zod'

/**
 * Sentinel `<Select>` value used by the transaction form to represent
 * "no payment method linked". The Server Action strips it to `undefined`
 * before sending the payload to the backend.
 */
export const NO_PAYMENT_METHOD = '__none__'

/**
 * Zod validation schema for the transaction form.
 *
 * - `amount`: decimal string matching `/^\d+(\.\d{1,2})?$/` (e.g. `"100"` or `"99.50"`), must be > 0
 * - `type`: one of `"INCOME"` | `"EXPENSE"`
 * - `description`: optional string, max 255 characters
 * - `date`: ISO-8601 datetime string (validated by zod's `datetime()`)
 * - `categoryId`: UUID string
 */
export const transactionSchema = z.object({
  amount: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, 'Введите корректную сумму (например: 100 или 100.50)')
    .refine((v) => parseFloat(v) > 0, 'Сумма должна быть больше 0'),
  type: z.enum(['INCOME', 'EXPENSE']),
  description: z.string().max(255, 'Описание не более 255 символов').optional().nullable(),
  date: z.string().datetime({ error: 'Введите корректную дату' }),
  categoryId: z.string().uuid('Выберите категорию'),
  paymentMethodId: z
    .union([
      z.string().uuid('Некорректный метод оплаты'),
      z.literal(''),
      z.literal(NO_PAYMENT_METHOD),
    ])
    .optional()
    .transform((v) => (v === '' || v === NO_PAYMENT_METHOD || v == null ? undefined : v)),
})

/**
 * Input type (pre-transform) — used for `useForm<TransactionFormValues>`.
 * `z.input` gives the shape react-hook-form manages; Server Actions re-parse to get the
 * transformed output (e.g. `paymentMethodId` normalised to `undefined` when empty).
 */
export type TransactionFormValues = z.input<typeof transactionSchema>
