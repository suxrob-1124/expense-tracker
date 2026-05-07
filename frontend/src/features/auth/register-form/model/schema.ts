import { z } from 'zod'

export const registerSchema = z.object({
  email: z.string().email('Введите корректный email'),
  password: z
    .string()
    .min(12, 'Пароль должен содержать минимум 12 символов')
    .max(128, 'Пароль не должен превышать 128 символов'),
  firstName: z.string().max(100, 'Имя не должно превышать 100 символов'),
  lastName: z.string().max(100, 'Фамилия не должна превышать 100 символов'),
  acceptTerms: z.boolean().refine((v) => v === true, {
    message: 'Необходимо принять условия соглашения',
  }),
})

export type RegisterFormValues = z.infer<typeof registerSchema>
