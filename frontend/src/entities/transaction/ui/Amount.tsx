import { TransactionType } from '../model/types'

interface AmountProps {
  value: string
  type: TransactionType
}

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
