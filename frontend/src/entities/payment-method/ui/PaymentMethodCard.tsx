'use client'

import { toast } from 'sonner'
import { Trash2, Archive, ArchiveRestore } from 'lucide-react'
import { PaymentMethodIcon } from '@/shared/ui/payment-method-icon'
import { formatMoney } from '@/shared/lib/formatMoney'
import type { PaymentMethodResponse } from '../model/types'

/** Props for the {@link PaymentMethodCard} component. */
interface PaymentMethodCardProps {
  /** The payment method to display. */
  paymentMethod: PaymentMethodResponse
  /**
   * Called when the user confirms deletion.
   * @param id - UUID of the payment method.
   * @returns An object with an optional `error` message on failure.
   */
  onDelete: (id: string) => Promise<{ error?: string }>
  /**
   * Called when the user toggles archive state. Receives the new desired value.
   * @param id - UUID of the payment method.
   * @param archived - New archive state.
   */
  onToggleArchive: (id: string, archived: boolean) => Promise<{ error?: string }>
}

/**
 * Client Component that renders a single payment method as a card.
 *
 * Shows the type-specific icon (via {@link PaymentMethodIcon}), the display name,
 * a masked `•••• 1234` last4 suffix when present, and the current balance.
 * On hover, exposes archive-toggle and delete actions. Archived methods are
 * rendered with reduced opacity to remain visible but visually de-emphasised.
 */
export function PaymentMethodCard({
  paymentMethod,
  onDelete,
  onToggleArchive,
}: PaymentMethodCardProps) {
  async function handleDelete() {
    if (!window.confirm(`Удалить метод «${paymentMethod.name}»?`)) return
    const result = await onDelete(paymentMethod.id)
    if (result.error) toast.error(result.error)
  }

  async function handleToggleArchive() {
    const result = await onToggleArchive(paymentMethod.id, !paymentMethod.archived)
    if (result.error) toast.error(result.error)
  }

  return (
    <div
      className={`flex flex-col gap-2 rounded-xl border p-4 relative group ${
        paymentMethod.archived ? 'opacity-50' : ''
      }`}
    >
      <div className="flex items-center gap-2">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-muted">
          <PaymentMethodIcon type={paymentMethod.type} size={20} />
        </div>
        <div className="flex flex-col min-w-0">
          <span className="text-sm font-medium truncate">{paymentMethod.name}</span>
          {paymentMethod.last4 && (
            <span className="text-xs text-muted-foreground">•••• {paymentMethod.last4}</span>
          )}
        </div>
      </div>

      {paymentMethod.balance != null && (
        <div className="text-sm font-mono text-right">
          {formatMoney(paymentMethod.balance)}
        </div>
      )}

      <div className="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={handleToggleArchive}
          className="text-muted-foreground hover:text-foreground"
          aria-label={paymentMethod.archived ? 'Разархивировать' : 'Архивировать'}
        >
          {paymentMethod.archived ? <ArchiveRestore size={14} /> : <Archive size={14} />}
        </button>
        <button
          onClick={handleDelete}
          className="text-muted-foreground hover:text-destructive"
          aria-label={`Удалить ${paymentMethod.name}`}
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  )
}
