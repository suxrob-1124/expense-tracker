'use server'

import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { backendFetch } from '@/shared/api/http'
import { parseProblemDetail, formatProblemMessage, type ProblemDetail } from '@/shared/api/problem'
import { API } from '@/shared/api/endpoints'
import type { AuthResponse, LoginRequest } from '@/shared/api/dto'
import { loginSchema } from '../model/schema'

/**
 * Discriminated union returned by {@link loginAction}.
 * `{ ok: false }` carries a user-facing error message; `{ ok: true }` means
 * the action redirected to `/transactions` (the branch is never reached by the caller).
 */
export type LoginActionResult = { ok: false; message: string } | { ok: true }

/**
 * Server Action — authenticates the user with email + password.
 *
 * Validates the payload against {@link loginSchema}, calls `POST /api/v1/auth/login`,
 * stores tokens as HttpOnly cookies via {@link setAuthCookies}, then redirects to
 * `/transactions` on success.
 *
 * @param raw - Raw form values; validated internally before the network call.
 * @returns `{ ok: false, message }` on validation or server error; otherwise redirects.
 */
export async function loginAction(raw: LoginRequest): Promise<LoginActionResult> {
  const parsed = loginSchema.safeParse(raw)
  if (!parsed.success) {
    return { ok: false, message: parsed.error.issues[0]?.message ?? 'Ошибка валидации' }
  }

  let res: Response
  try {
    res = await backendFetch(API.auth.login, {
      method: 'POST',
      body: JSON.stringify(parsed.data),
    })
  } catch {
    return { ok: false, message: 'Не удалось подключиться к серверу' }
  }

  if (!res.ok) {
    const problem: ProblemDetail | null = await parseProblemDetail(res)
    if (res.status === 401) return { ok: false, message: 'Неверный email или пароль' }
    if (res.status === 423) return { ok: false, message: 'Аккаунт временно заблокирован. Попробуйте позже' }
    return { ok: false, message: problem ? formatProblemMessage(problem) : `Ошибка ${res.status}` }
  }

  const data: AuthResponse = await res.json()
  await setAuthCookies(res, data)

  redirect('/transactions')
}

/**
 * Proxies the backend `Set-Cookie` refresh token header and sets the access token
 * as an HttpOnly cookie in the Next.js cookie jar.
 *
 * Called from {@link loginAction} and `registerAction` after a successful auth response.
 *
 * @param res  - The raw `Response` from the backend (used to forward the `Set-Cookie` header).
 * @param data - The parsed {@link AuthResponse} containing access-token details.
 */
export async function setAuthCookies(res: Response, data: AuthResponse) {
  const jar = await cookies()

  // Proxy refreshToken from backend Set-Cookie
  const setCookie = res.headers.get('set-cookie') ?? ''
  const rtMatch = setCookie.match(/refreshToken=([^;]+)/)
  if (rtMatch) {
    jar.set('refreshToken', rtMatch[1], {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      path: '/api/v1/auth',
      maxAge: data.expiresInSeconds * 48, // refresh TTL is longer (7 days = 48x access TTL)
    })
  }

  // Store accessToken as HttpOnly cookie
  jar.set('accessToken', data.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict',
    path: '/',
    maxAge: data.expiresInSeconds,
  })
}
