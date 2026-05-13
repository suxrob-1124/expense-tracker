import { ArrowUpRight, ArrowDownLeft, Scale } from 'lucide-react'
import { Card, CardContent } from '@/shared/ui/card'

interface TransactionsKpiProps {
  /** Total income for the period as a decimal string. Example: `"1500.0000"` */
  income: string
  /** Total expenses for the period as a decimal string. Example: `"800.0000"` */
  expense: string
  /** Net balance (income − expense) as a decimal string. Example: `"700.0000"` */
  balance: string
}

const fmt = (value: string) =>
  new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(Number(value))

/**
 * Displays three KPI cards: Income (green), Expenses (red), and Balance.
 *
 * Balance card colour adapts: red when negative, green otherwise.
 * Values are formatted as RUB currency with `Intl.NumberFormat` (`ru-RU`).
 */
export function TransactionsKpi({ income, expense, balance }: TransactionsKpiProps) {
  const isNegativeBalance = balance.startsWith('-')

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card>
        <CardContent className="flex items-center gap-4 p-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-100">
            <ArrowUpRight size={20} className="text-green-600" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Доходы</p>
            <p className="text-lg font-semibold text-green-600">{fmt(income)}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex items-center gap-4 p-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-100">
            <ArrowDownLeft size={20} className="text-red-600" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Расходы</p>
            <p className="text-lg font-semibold text-red-600">{fmt(expense)}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex items-center gap-4 p-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-muted">
            <Scale size={20} className="text-foreground" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Баланс</p>
            <p className={`text-lg font-semibold ${isNegativeBalance ? 'text-red-600' : 'text-green-600'}`}>
              {fmt(balance)}
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
