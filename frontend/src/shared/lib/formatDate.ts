export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ru-RU')
}

export function formatMonthYear(month: number, year: number): string {
  const date = new Date(year, month - 1, 1)
  const formatted = new Intl.DateTimeFormat('ru-RU', { month: 'long', year: 'numeric' }).format(date)
  return formatted.charAt(0).toUpperCase() + formatted.slice(1)
}
