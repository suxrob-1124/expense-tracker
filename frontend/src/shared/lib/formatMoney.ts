/**
 * Formats a backend decimal-string monetary value (scale 4) as a localised
 * currency string in `ru-RU` / RUB.
 *
 * Use for neutral balances (no INCOME/EXPENSE direction). For signed
 * transaction amounts use `<Amount>` from `entities/transaction`.
 *
 * @param value - decimal string from the backend, e.g. `"1000.0000"`. Falsy values yield an empty string.
 * @returns Formatted currency string, e.g. `"1 000,00 ₽"`.
 *
 * @example
 * formatMoney("1000.0000") // "1 000,00 ₽"
 * formatMoney(null)        // ""
 */
export function formatMoney(value: string | null | undefined): string {
  if (!value) return ''
  const parsed = Number.parseFloat(value)
  if (Number.isNaN(parsed)) return ''
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
  }).format(parsed)
}
