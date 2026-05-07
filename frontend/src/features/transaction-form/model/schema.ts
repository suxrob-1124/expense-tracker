import { z } from 'zod'

export const transactionSchema = z.object({
  amount: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, 'Введите корректную сумму (например: 100 или 100.50)')
    .refine((v) => parseFloat(v) > 0, 'Сумма должна быть больше 0'),
  type: z.enum(['INCOME', 'EXPENSE']),
  description: z.string().max(255, 'Описание не более 255 символов').optional().nullable(),
  date: z.string().datetime({ message: 'Введите корректную дату' }),
  categoryId: z.string().uuid('Выберите категорию'),
})

export type TransactionFormValues = z.infer<typeof transactionSchema>
