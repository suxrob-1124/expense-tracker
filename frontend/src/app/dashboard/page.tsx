import { logoutAction } from './actions'

export default function DashboardPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 p-8">
      <h1 className="text-3xl font-bold">Dashboard</h1>
      <p className="text-muted-foreground">TODO — Шаг 7</p>
      <form action={logoutAction}>
        <button
          type="submit"
          className="rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground"
        >
          Выйти
        </button>
      </form>
    </main>
  )
}
