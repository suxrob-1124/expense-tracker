'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/shared/lib/cn'
import { logoutAction } from '@/features/auth/logout'
import { Wallet, ArrowLeftRight, Folder, User, LogOut } from 'lucide-react'

const NAV_ITEMS = [
  { href: '/transactions', label: 'Транзакции', Icon: ArrowLeftRight },
  { href: '/categories', label: 'Категории', Icon: Folder },
  { href: '/profile', label: 'Профиль', Icon: User },
]

/** Props for the {@link SidebarNav} component. */
interface SidebarNavProps {
  /** Current user data displayed in the sidebar footer. */
  user: {
    /** First name used for the avatar initial and display name. */
    firstName: string
    /** Email address shown below the display name. */
    email: string
  }
}

/**
 * Client Component that renders the main application navigation sidebar.
 *
 * Highlights the active route via `usePathname`. Shows navigation items for
 * Transactions, Categories and Profile. Renders a user card with a logout button
 * at the bottom (desktop only). Collapses to a horizontal top bar on mobile.
 */
export function SidebarNav({ user }: SidebarNavProps) {
  const pathname = usePathname()

  return (
    <aside
      aria-label="Главная навигация"
      className="flex md:flex-col md:w-60 md:min-h-screen md:fixed md:left-0 md:top-0 md:border-r md:bg-background
                 flex-row w-full border-b bg-background px-2 py-2 gap-1 md:px-4 md:py-6 md:gap-1 z-10"
    >
      <Link
        href="/transactions"
        className="hidden md:flex items-center gap-2 px-2 mb-6 text-lg font-semibold"
      >
        <Wallet size={20} />
        Expense Tracker
      </Link>

      <nav className="flex flex-row md:flex-col gap-1 flex-1 md:flex-none">
        {NAV_ITEMS.map(({ href, label, Icon }) => (
          <Link
            key={href}
            href={href}
            className={cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground',
              pathname === href
                ? 'bg-accent text-accent-foreground'
                : 'text-muted-foreground'
            )}
          >
            <Icon size={16} />
            <span className="hidden md:inline">{label}</span>
          </Link>
        ))}
      </nav>

      <div className="hidden md:flex flex-grow" />

      <div className="hidden md:flex items-center gap-3 px-2 py-2 border-t">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
          {user.firstName.charAt(0).toUpperCase()}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{user.firstName}</p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
        </div>
        <form action={logoutAction}>
          <button
            type="submit"
            className="text-muted-foreground hover:text-foreground transition-colors"
            aria-label="Выйти"
          >
            <LogOut size={16} />
          </button>
        </form>
      </div>
    </aside>
  )
}
