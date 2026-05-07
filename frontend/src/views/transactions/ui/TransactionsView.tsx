import Link from 'next/link'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { TransactionResponse, CategoryResponse } from '@/shared/api/dto'
import { Amount } from '@/entities/transaction'
import { formatDate, formatMonthYear } from '@/shared/lib/formatDate'
import { TransactionsKpi } from '@/widgets/transactions-kpi'
import { MonthSwitcher } from './MonthSwitcher'
import { NewTransactionButton } from './NewTransactionButton'
import { Card, CardContent } from '@/shared/ui/card'

interface TransactionsViewProps {
  month: number
  year: number
}

export async function TransactionsView({ month, year }: TransactionsViewProps) {
  const [txRes, catRes] = await Promise.all([
    backendFetch(API.transactions.list(month, year), { forwardAccessToken: true }),
    backendFetch(API.categories.base, { forwardAccessToken: true }),
  ])

  const transactions: TransactionResponse[] = txRes.ok ? await txRes.json() : []
  const categories: CategoryResponse[] = catRes.ok ? await catRes.json() : []

  const income = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((s, t) => s + parseFloat(t.amount), 0)
  const expense = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((s, t) => s + parseFloat(t.amount), 0)

  const noCategories = categories.length === 0

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">Транзакции</h1>
          <p className="text-sm text-muted-foreground mt-0.5">{formatMonthYear(month, year)}</p>
        </div>
        <NewTransactionButton categories={categories} disabled={noCategories} />
      </div>

      <TransactionsKpi income={income} expense={expense} />

      <div className="flex items-center">
        <MonthSwitcher month={month} year={year} />
      </div>

      {noCategories ? (
        <Card>
          <CardContent className="py-12 text-center space-y-2">
            <p className="text-muted-foreground">Сначала создайте категорию</p>
            <Link href="/categories" className="text-sm underline underline-offset-4">
              Перейти к категориям
            </Link>
          </CardContent>
        </Card>
      ) : transactions.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Транзакций за этот месяц нет</p>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <ul className="divide-y">
            {transactions.map((tx) => {
              const category = categories.find((c) => c.id === tx.categoryId)
              return (
                <li key={tx.id} className="flex items-center justify-between px-4 py-3">
                  <div className="flex flex-col gap-0.5">
                    <span className="text-sm font-medium">
                      {category?.name ?? 'Без категории'}
                    </span>
                    {tx.description && (
                      <span className="text-xs text-muted-foreground">{tx.description}</span>
                    )}
                    <span className="text-xs text-muted-foreground">{formatDate(tx.date)}</span>
                  </div>
                  <Amount value={tx.amount} type={tx.type} />
                </li>
              )
            })}
          </ul>
        </Card>
      )}
    </div>
  )
}
