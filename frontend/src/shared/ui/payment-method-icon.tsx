import { CreditCard, Banknote, Landmark } from 'lucide-react'
import type { PaymentMethodType } from '@/shared/api/dto'

/** Per-type icon component lookup for payment methods. */
const ICONS = {
  CARD: CreditCard,
  CASH: Banknote,
  BANK: Landmark,
} as const

/** Props for {@link PaymentMethodIcon}. */
interface PaymentMethodIconProps {
  /** Payment method type used to pick the icon. */
  type: PaymentMethodType
  /** Icon size in pixels. Defaults to `16`. */
  size?: number
  /** Optional className passed through to the underlying SVG. */
  className?: string
}

/**
 * Renders a Lucide icon corresponding to a {@link PaymentMethodType}.
 *
 * Shared in `shared/ui` so it can be reused by entities (cards), features (form
 * selects), and any future widget that needs to display a payment method.
 */
export function PaymentMethodIcon({ type, size = 16, className }: PaymentMethodIconProps) {
  const Icon = ICONS[type]
  return <Icon size={size} className={className} aria-hidden />
}
