'use server'

import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'

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
