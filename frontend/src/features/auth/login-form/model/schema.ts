import { z } from 'zod'

/** Zod schema for the login form. Validates email format and requires a non-empty password. */
export const loginSchema = z.object({
  email: z.string().email('Введите корректный email'),
  password: z.string().min(1, 'Введите пароль'),
})

/** Inferred type of a valid login form submission. */
export type LoginFormValues = z.infer<typeof loginSchema>
