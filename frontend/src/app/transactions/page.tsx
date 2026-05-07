import { TransactionsView } from '@/views/transactions'

interface TransactionsPageProps {
  searchParams: Promise<{ month?: string; year?: string }>
}

export default async function TransactionsPage({ searchParams }: TransactionsPageProps) {
  const params = await searchParams
  const now = new Date()
  const month = params.month ? parseInt(params.month, 10) : now.getMonth() + 1
  const year = params.year ? parseInt(params.year, 10) : now.getFullYear()

  return <TransactionsView month={month} year={year} />
}
