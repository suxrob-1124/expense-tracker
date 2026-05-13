'use client'

import { useState, useMemo } from 'react'
import { PaymentMethodCard, type PaymentMethodResponse } from '@/entities/payment-method'
import {
  deletePaymentMethodAction,
  toggleArchivePaymentMethodAction,
} from '@/features/payment-method-form'

/** Props for {@link PaymentMethodsList}. */
interface PaymentMethodsListProps {
  /** Full list of payment methods (active + archived) fetched server-side. */
  paymentMethods: PaymentMethodResponse[]
}

/**
 * Client Component — renders the payment-methods grid with an inline filter
 * for archived items.
 *
 * Archived methods are hidden by default to keep the default view clean as the
 * collection grows. A toggle reveals them on demand. Counts of archived items
 * are shown next to the toggle to make the filter discoverable.
 */
export function PaymentMethodsList({ paymentMethods }: PaymentMethodsListProps) {
  const [showArchived, setShowArchived] = useState(false)

  const archivedCount = useMemo(
    () => paymentMethods.filter((pm) => pm.archived).length,
    [paymentMethods],
  )

  const visible = showArchived
    ? paymentMethods
    : paymentMethods.filter((pm) => !pm.archived)

  const noActive = paymentMethods.length > 0 && archivedCount === paymentMethods.length

  return (
    <div className="space-y-4">
      {archivedCount > 0 && (
        <label className="flex items-center gap-2 text-sm text-muted-foreground cursor-pointer select-none">
          <input
            type="checkbox"
            checked={showArchived}
            onChange={(e) => setShowArchived(e.target.checked)}
            className="h-4 w-4"
          />
          Показать архивные ({archivedCount})
        </label>
      )}

      {visible.length === 0 ? (
        <p className="text-muted-foreground text-center py-8">
          {noActive
            ? 'Все методы оплаты архивированы'
            : 'Методов оплаты пока нет — создайте первый ниже'}
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {visible.map((pm) => (
            <PaymentMethodCard
              key={pm.id}
              paymentMethod={pm}
              onDelete={deletePaymentMethodAction}
              onToggleArchive={toggleArchivePaymentMethodAction}
            />
          ))}
        </div>
      )}
    </div>
  )
}
