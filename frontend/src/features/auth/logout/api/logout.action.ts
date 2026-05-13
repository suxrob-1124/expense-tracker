'use server'

import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'

/**
 * Server Action — logs the user out.
 *
 * Calls `POST /api/v1/auth/logout` to revoke the refresh token on the backend,
 * then deletes both `accessToken` and `refreshToken` cookies regardless of whether
 * the backend call succeeded (network failures are swallowed). Redirects to `/login`.
 */
export async function logoutAction() {
  try {
    await backendFetch(API.auth.logout, {
      method: 'POST',
      forwardRefreshCookie: true,
    })
  } catch {
    // proceed with local cleanup even if backend is unreachable
  }

  const jar = await cookies()
  jar.delete('accessToken')
  jar.delete('refreshToken')

  redirect('/login')
}
