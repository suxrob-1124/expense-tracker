'use client'

import { useState } from 'react'
import { TransactionForm } from '@/features/transaction-form'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import type { CategoryResponse } from '@/shared/api/dto'

interface NewTransactionButtonProps {
  /** Category list passed down from the parent Server Component for the inline form */
  categories: CategoryResponse[]
  /** Disables the button when no categories exist — a transaction requires a category */
  disabled?: boolean
}

/**
 * Client Component — toggles an inline transaction creation form.
 *
 * Renders a button when collapsed; expands to an inline {@link TransactionForm}
 * card on click. Collapses again after a successful submission or cancellation.
 */
export function NewTransactionButton({ categories, disabled }: NewTransactionButtonProps) {
  const [open, setOpen] = useState(false)

  if (!open) {
    return (
      <Button onClick={() => setOpen(true)} disabled={disabled}>
        + Новая транзакция
      </Button>
    )
  }

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Новая транзакция</CardTitle>
      </CardHeader>
      <CardContent>
        <TransactionForm
          categories={categories}
          onSuccess={() => setOpen(false)}
        />
        <Button
          variant="ghost"
          className="mt-2 w-full"
          onClick={() => setOpen(false)}
        >
          Отмена
        </Button>
      </CardContent>
    </Card>
  )
}
