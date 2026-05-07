import { Amount } from './Amount'
import { formatDate } from '@/shared/lib/formatDate'
import type { TransactionResponse } from '@/shared/api/dto'

interface TransactionRowProps {
  transaction: TransactionResponse
}

export function TransactionRow({ transaction }: TransactionRowProps) {
  return (
    <li className="flex items-center justify-between px-4 py-3">
      <div className="flex flex-col gap-0.5">
        {transaction.description && (
          <span className="text-sm font-medium">{transaction.description}</span>
        )}
        <span className="text-xs text-muted-foreground">
          {formatDate(transaction.date)}
        </span>
      </div>
      <Amount value={transaction.amount} type={transaction.type} />
    </li>
  )
}
