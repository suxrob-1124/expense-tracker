'use server'

import { redirect } from 'next/navigation'
import { backendFetch } from '@/shared/api/http'
import { parseProblemDetail, formatProblemMessage, type ProblemDetail } from '@/shared/api/problem'
import { API } from '@/shared/api/endpoints'
import type { AuthResponse, RegisterRequest } from '@/shared/api/dto'
import type { RegisterFormValues } from '../model/schema'
import { registerSchema } from '../model/schema'
import { setAuthCookies } from '@/features/auth/login-form'

/**
 * Discriminated union returned by {@link registerAction}.
 * `{ ok: false }` carries a user-facing error message.
 */
export type RegisterActionResult = { ok: false; message: string } | { ok: true }

/**
 * Server Action — registers a new user and immediately logs them in.
 *
 * Validates the payload against {@link registerSchema}, strips `acceptTerms` before
 * calling `POST /api/v1/users/register`, then auto-logs in via `POST /api/v1/auth/login`
 * and redirects to `/transactions`.
 *
 * @param raw - Raw form values including `acceptTerms` (stripped before the API call).
 * @returns `{ ok: false, message }` on error; otherwise redirects (branch unreachable).
 */
export async function registerAction(raw: RegisterFormValues): Promise<RegisterActionResult> {
  const parsed = registerSchema.safeParse(raw)
  if (!parsed.success) {
    return { ok: false, message: parsed.error.issues[0]?.message ?? 'Ошибка валидации' }
  }

  // Strip acceptTerms — not part of the backend RegisterRequest DTO
  const { acceptTerms: _, ...backendPayload } = parsed.data

  let res: Response
  try {
    res = await backendFetch(API.users.register, {
      method: 'POST',
      body: JSON.stringify(backendPayload),
    })
  } catch {
    return { ok: false, message: 'Не удалось подключиться к серверу' }
  }

  if (!res.ok) {
    const problem: ProblemDetail | null = await parseProblemDetail(res)
    return { ok: false, message: problem ? formatProblemMessage(problem) : `Ошибка ${res.status}` }
  }

  // Register returns UserResponse (201), not AuthResponse — auto-login after registration
  const loginRes = await backendFetch(API.auth.login, {
    method: 'POST',
    body: JSON.stringify({ email: parsed.data.email, password: parsed.data.password }),
  })

  if (loginRes.ok) {
    const data: AuthResponse = await loginRes.json()
    await setAuthCookies(loginRes, data)
  }

  redirect('/transactions')
}
