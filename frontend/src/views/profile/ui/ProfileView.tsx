import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { UserResponse } from '@/shared/api/dto'
import { logoutAction } from '@/features/auth/logout'
import { formatDate } from '@/shared/lib/formatDate'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'

export async function ProfileView() {
  const res = await backendFetch(API.users.me, { forwardAccessToken: true })
  const user: UserResponse = await res.json()

  const fields = [
    { label: 'Имя', value: user.firstName },
    { label: 'Фамилия', value: user.lastName },
    { label: 'Email', value: user.email },
    { label: 'Роль', value: user.role },
    { label: 'С нами с', value: formatDate(user.createdAt) },
  ]

  return (
    <div className="space-y-6 max-w-lg">
      <div>
        <h1 className="text-2xl font-bold">Профиль</h1>
        <p className="text-sm text-muted-foreground mt-0.5">Данные вашего аккаунта</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Личные данные</CardTitle>
        </CardHeader>
        <CardContent className="divide-y">
          {fields.map(({ label, value }) => (
            <div key={label} className="flex justify-between py-3">
              <span className="text-sm text-muted-foreground">{label}</span>
              <span className="text-sm font-medium">{value}</span>
            </div>
          ))}
        </CardContent>
      </Card>

      <form action={logoutAction}>
        <Button type="submit" variant="outline">
          Выйти
        </Button>
      </form>
    </div>
  )
}
