import { TransactionType } from '../model/types'

interface AmountProps {
  /** Decimal string representing the monetary value. Example: `"99.9900"` */
  value: string
  /** Determines sign prefix and colour: green `+` for INCOME, red `−` for EXPENSE */
  type: TransactionType
}

/**
 * Renders a formatted currency amount with a sign prefix and colour.
 *
 * - INCOME: green text, `+` prefix
 * - EXPENSE: red text, `−` prefix
 *
 * Formatting uses `Intl.NumberFormat` with `ru-RU` locale and RUB currency.
 */
export function Amount({ value, type }: AmountProps) {
  const formatted = new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
  }).format(parseFloat(value))

  const colorClass = type === 'INCOME' ? 'text-green-600' : 'text-red-600'
  const prefix = type === 'INCOME' ? '+' : '−'

  return (
    <span className={colorClass}>
      {prefix}{formatted}
    </span>
  )
}
