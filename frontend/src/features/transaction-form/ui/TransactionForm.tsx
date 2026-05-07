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
import { transactionSchema, type TransactionFormValues } from '../model/schema'
import { createTransactionAction, updateTransactionAction } from '../api/transaction.action'
import type { TransactionResponse, CategoryResponse } from '@/shared/api/dto'

interface TransactionFormProps {
  categories: CategoryResponse[]
  initialValues?: TransactionResponse
  onSuccess?: () => void
}

export function TransactionForm({ categories, initialValues, onSuccess }: TransactionFormProps) {
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
        }
      : {
          amount: '',
          type: 'EXPENSE',
          description: '',
          date: new Date().toISOString(),
          categoryId: '',
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
