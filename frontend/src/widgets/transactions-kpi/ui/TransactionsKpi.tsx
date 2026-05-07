import { ArrowUpRight, ArrowDownLeft, Scale } from 'lucide-react'
import { Card, CardContent } from '@/shared/ui/card'

interface TransactionsKpiProps {
  income: number
  expense: number
}

const fmt = (n: number) =>
  new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(n)

export function TransactionsKpi({ income, expense }: TransactionsKpiProps) {
  const balance = income - expense

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
            <p className={`text-lg font-semibold ${balance >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {fmt(balance)}
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
