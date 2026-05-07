'use client'

import { useState } from 'react'
import { TransactionForm } from '@/features/transaction-form'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import type { CategoryResponse } from '@/shared/api/dto'

interface NewTransactionButtonProps {
  categories: CategoryResponse[]
  disabled?: boolean
}

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
