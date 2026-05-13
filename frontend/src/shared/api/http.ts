import 'server-only'
import { cookies } from 'next/headers'
import { env } from '@/shared/config/env'

/** Options accepted by {@link backendFetch} in addition to the standard `RequestInit`. */
interface BackendFetchOptions extends Omit<RequestInit, 'headers'> {
  /** Extra HTTP headers merged on top of the default `Content-Type: application/json`. */
  headers?: Record<string, string>
  /**
   * When `true`, reads the `refreshToken` cookie from the incoming request and forwards it
   * as a `Cookie` header to the backend.
   *
   * Use this only for the `/api/v1/auth/refresh` proxy call.
   */
  forwardRefreshCookie?: boolean
  /**
   * When `true`, reads the `accessToken` cookie and attaches it as a
   * `Authorization: Bearer <token>` header.
   *
   * Required for all authenticated backend calls made from Server Components or Server Actions.
   */
  forwardAccessToken?: boolean
}

/**
 * Server-only fetch wrapper for all backend API calls.
 *
 * <p>This module is tagged with `import 'server-only'` — it cannot be imported in Client
 * Components. All HTTP calls to the Spring Boot backend must go through this function so
 * that credentials (cookies) are attached server-side and never exposed to the browser.
 *
 * @param path    The backend path, e.g. `/api/v1/users/me`. Prepended with
 *                `BACKEND_INTERNAL_URL` from the server-side environment.
 * @param options Fetch options plus `forwardRefreshCookie` / `forwardAccessToken` flags.
 * @returns The raw `Response` — callers are responsible for checking `res.ok` and parsing.
 *
 * @example
 * const res = await backendFetch('/api/v1/users/me', { forwardAccessToken: true })
 * if (!res.ok) { ... }
 * const user: UserResponse = await res.json()
 */
export async function backendFetch(path: string, options: BackendFetchOptions = {}): Promise<Response> {
  const { forwardRefreshCookie, forwardAccessToken, headers: extraHeaders, ...init } = options
  const headers = new Headers(extraHeaders)
  headers.set('Content-Type', 'application/json')

  const jar = await cookies()

  if (forwardRefreshCookie) {
    const rt = jar.get('refreshToken')?.value
    if (rt) headers.set('Cookie', `refreshToken=${rt}`)
  }

  if (forwardAccessToken) {
    const at = jar.get('accessToken')?.value
    if (at) headers.set('Authorization', `Bearer ${at}`)
  }

  return fetch(`${env.BACKEND_INTERNAL_URL}${path}`, { ...init, headers })
}
