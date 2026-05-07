import 'server-only'
import { cookies } from 'next/headers'
import { env } from '@/shared/config/env'

interface BackendFetchOptions extends Omit<RequestInit, 'headers'> {
  headers?: Record<string, string>
  forwardRefreshCookie?: boolean
  forwardAccessToken?: boolean
}

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
