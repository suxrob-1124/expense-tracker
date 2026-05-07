import 'server-only'
import { cookies } from 'next/headers'
import { env } from '@/shared/config/env'

interface BackendFetchOptions extends Omit<RequestInit, 'headers'> {
  headers?: Record<string, string>
  forwardRefreshCookie?: boolean
}

export async function backendFetch(path: string, options: BackendFetchOptions = {}): Promise<Response> {
  const { forwardRefreshCookie, headers: extraHeaders, ...init } = options
  const headers = new Headers(extraHeaders)
  headers.set('Content-Type', 'application/json')

  if (forwardRefreshCookie) {
    const rt = (await cookies()).get('refreshToken')?.value
    if (rt) headers.set('Cookie', `refreshToken=${rt}`)
  }

  return fetch(`${env.BACKEND_INTERNAL_URL}${path}`, { ...init, headers })
}
