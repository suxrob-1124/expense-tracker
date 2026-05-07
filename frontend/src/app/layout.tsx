import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import { Toaster } from '@/shared/ui/sonner'

const inter = Inter({ subsets: ['latin', 'cyrillic'] })

export const metadata: Metadata = {
  title: 'Expense Tracker',
  description: 'Корпоративный трекер расходов',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru" className={inter.className}>
      <body>
        {children}
        <Toaster />
      </body>
    </html>
  )
}
