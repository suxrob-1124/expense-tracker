'use client'

import { useRouter } from 'next/navigation'

interface MonthSwitcherProps {
  /** Current 1-based month (1–12) */
  month: number
  /** Current calendar year */
  year: number
}

const MONTH_NAMES = [
  'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
  'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь',
]

/**
 * Client Component — previous/next navigation for the monthly transaction view.
 *
 * Pushes a new route to `/transactions?month=M&year=Y` on each click,
 * triggering a Server Component re-fetch in {@link TransactionsView}.
 */
export function MonthSwitcher({ month, year }: MonthSwitcherProps) {
  const router = useRouter()

  const navigate = (offsetMonths: number) => {
    const date = new Date(year, month - 1 + offsetMonths, 1)
    const m = date.getMonth() + 1
    const y = date.getFullYear()
    router.push(`/transactions?month=${m}&year=${y}`)
  }

  return (
    <div className="flex items-center gap-3">
      <button
        onClick={() => navigate(-1)}
        className="rounded-md border border-input px-3 py-1 text-sm hover:bg-accent"
        aria-label="Предыдущий месяц"
      >
        ←
      </button>
      <span className="min-w-[140px] text-center font-medium">
        {MONTH_NAMES[month - 1]} {year}
      </span>
      <button
        onClick={() => navigate(1)}
        className="rounded-md border border-input px-3 py-1 text-sm hover:bg-accent"
        aria-label="Следующий месяц"
      >
        →
      </button>
    </div>
  )
}
