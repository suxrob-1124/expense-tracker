import { Amount } from './Amount'
import { formatDate } from '@/shared/lib/formatDate'
import type { TransactionResponse } from '@/shared/api/dto'

interface TransactionRowProps {
  /** Full transaction object to render */
  transaction: TransactionResponse
}

/**
 * Renders a single transaction as a list item (`<li>`).
 *
 * Displays the optional description, formatted date, and a coloured {@link Amount}.
 * Intended for use inside a `<ul>` with `divide-y` styling.
 */
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
