import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { TransactionResponse, CategoryResponse } from '@/shared/api/dto'
import { Amount } from '@/entities/transaction'
import { MonthSwitcher } from './MonthSwitcher'
import { NewTransactionButton } from './NewTransactionButton'

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

  return (
    <main className="mx-auto max-w-2xl space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Транзакции</h1>
        <NewTransactionButton categories={categories} />
      </div>

      <div className="flex items-center justify-between rounded-lg border p-4">
        <MonthSwitcher month={month} year={year} />
        <div className="flex gap-6 text-sm">
          <span className="text-green-600">
            +{new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' }).format(income)}
          </span>
          <span className="text-red-600">
            −{new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' }).format(expense)}
          </span>
        </div>
      </div>

      {transactions.length === 0 ? (
        <p className="text-center text-muted-foreground py-12">
          Транзакций за этот месяц нет
        </p>
      ) : (
        <ul className="divide-y rounded-lg border">
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
                  <span className="text-xs text-muted-foreground">
                    {new Date(tx.date).toLocaleDateString('ru-RU')}
                  </span>
                </div>
                <Amount value={tx.amount} type={tx.type} />
              </li>
            )
          })}
        </ul>
      )}
    </main>
  )
}
