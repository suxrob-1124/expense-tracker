'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/shared/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select'
import Link from 'next/link'
import { transactionSchema, type TransactionFormValues } from '../model/schema'
import { createTransactionAction, updateTransactionAction } from '../api/transaction.action'
import type { TransactionResponse, CategoryResponse, PaymentMethodResponse } from '@/shared/api/dto'
import { PaymentMethodIcon } from '@/shared/ui/payment-method-icon'

interface TransactionFormProps {
  /** Available categories to populate the category selector */
  categories: CategoryResponse[]
  /**
   * Available payment methods to populate the optional selector.
   * Pass only non-archived methods. When the array is empty, the form
   * renders a deep link to `/payment-methods` instead of the selector.
   */
  paymentMethods?: PaymentMethodResponse[]
  /**
   * When provided the form operates in edit mode: fields are pre-filled and
   * submission calls {@link updateTransactionAction} instead of {@link createTransactionAction}.
   */
  initialValues?: TransactionResponse
  /** Called after a successful create or update */
  onSuccess?: () => void
}

/**
 * Controlled form for creating or editing a transaction.
 *
 * Uses `react-hook-form` with a zod resolver ({@link transactionSchema}).
 * Submits via Server Actions; shows toast notifications on success/failure.
 *
 * Render in a Client Component (`"use client"`) context.
 */
export function TransactionForm({
  categories,
  paymentMethods = [],
  initialValues,
  onSuccess,
}: TransactionFormProps) {
  const isEdit = !!initialValues

  const form = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionSchema),
    defaultValues: initialValues
      ? {
          amount: initialValues.amount,
          type: initialValues.type,
          description: initialValues.description ?? '',
          date: initialValues.date,
          categoryId: initialValues.categoryId,
          paymentMethodId: initialValues.paymentMethodId ?? '',
        }
      : {
          amount: '',
          type: 'EXPENSE',
          description: '',
          date: new Date().toISOString(),
          categoryId: '',
          paymentMethodId: '',
        },
  })

  const onSubmit = async (values: TransactionFormValues) => {
    const result = isEdit
      ? await updateTransactionAction(initialValues!.id, values)
      : await createTransactionAction(values)

    if (!result.ok) {
      toast.error(result.message)
      return
    }

    toast.success(isEdit ? 'Транзакция обновлена' : 'Транзакция создана')
    onSuccess?.()
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="amount"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Сумма</FormLabel>
              <FormControl>
                <Input type="text" placeholder="100.00" inputMode="decimal" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="type"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Тип</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Выберите тип" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="EXPENSE">Расход</SelectItem>
                  <SelectItem value="INCOME">Доход</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="categoryId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Категория</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="— выберите категорию —" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        {paymentMethods.length === 0 ? (
          <div className="space-y-1">
            <p className="text-sm font-medium">Метод оплаты</p>
            <p className="text-xs text-muted-foreground">
              Методов оплаты пока нет.{' '}
              <Link
                href="/payment-methods"
                className="underline underline-offset-4 hover:text-foreground"
              >
                Добавить метод
              </Link>{' '}
              (опционально).
            </p>
          </div>
        ) : (
          <FormField
            control={form.control}
            name="paymentMethodId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Метод оплаты</FormLabel>
                <Select
                  value={field.value && field.value !== '' ? field.value : '__none__'}
                  onValueChange={field.onChange}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="— без метода —" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="__none__">— без метода —</SelectItem>
                    {paymentMethods
                      .filter((pm) => !pm.archived)
                      .map((pm) => (
                        <SelectItem key={pm.id} value={pm.id}>
                          <span className="flex items-center gap-2">
                            <PaymentMethodIcon type={pm.type} size={14} />
                            {pm.name}
                            {pm.last4 && ` •••• ${pm.last4}`}
                          </span>
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        <FormField
          control={form.control}
          name="date"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Дата</FormLabel>
              <FormControl>
                <Input type="datetime-local" {...field} value={field.value?.slice(0, 16)} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Описание</FormLabel>
              <FormControl>
                <Input
                  type="text"
                  placeholder="Необязательно"
                  {...field}
                  value={field.value ?? ''}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting
            ? isEdit
              ? 'Сохранение...'
              : 'Создание...'
            : isEdit
              ? 'Сохранить'
              : 'Создать транзакцию'}
        </Button>
      </form>
    </Form>
  )
}
