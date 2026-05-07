import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { UserResponse } from '@/shared/api/dto'
import { SidebarNav } from '@/widgets/sidebar-nav'

export default async function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  const res = await backendFetch(API.users.me, { forwardAccessToken: true })
  const user: UserResponse = res.ok ? await res.json() : { firstName: '', email: '' } as UserResponse

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <SidebarNav user={{ firstName: user.firstName, email: user.email }} />
      <main className="flex-1 md:ml-60 p-6 md:p-8">{children}</main>
    </div>
  )
}
