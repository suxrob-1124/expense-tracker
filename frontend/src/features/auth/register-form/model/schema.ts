import { z } from 'zod'

/**
 * Zod schema for the registration form.
 * Password: min 12, max 128 characters.
 * `acceptTerms` must be `true`; it is stripped before sending to the backend.
 */
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

/** Inferred type of a valid registration form submission (includes `acceptTerms`). */
export type RegisterFormValues = z.infer<typeof registerSchema>
