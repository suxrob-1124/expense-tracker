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
import { PaymentMethodIcon } from '@/shared/ui/payment-method-icon'
import type { PaymentMethodType } from '@/entities/payment-method'
import { paymentMethodSchema, type PaymentMethodFormData } from '../model/schema'
import { createPaymentMethodAction } from '../api/create-payment-method.action'

const TYPE_OPTIONS: { value: PaymentMethodType; label: string }[] = [
  { value: 'CARD', label: 'Карта' },
  { value: 'CASH', label: 'Наличные' },
  { value: 'BANK', label: 'Банковский счёт' },
]

/**
 * Client Component — form for creating a payment method.
 *
 * Uses `react-hook-form` + zod ({@link paymentMethodSchema}). On submit calls
 * {@link createPaymentMethodAction} and surfaces backend errors via `sonner`.
 * Resets the form on success while keeping the previously selected `type`.
 */
export function PaymentMethodForm() {
  const form = useForm<PaymentMethodFormData>({
    resolver: zodResolver(paymentMethodSchema),
    defaultValues: { name: '', type: 'CARD', last4: '', balance: '' },
  })

  const onSubmit = async (values: PaymentMethodFormData) => {
    const result = await createPaymentMethodAction(values)
    if (result.error) {
      toast.error(result.error)
      return
    }
    toast.success('Метод оплаты создан')
    form.reset({ name: '', type: values.type, last4: '', balance: '' })
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название</FormLabel>
              <FormControl>
                <Input placeholder="Например, Visa Gold" {...field} />
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
                  {TYPE_OPTIONS.map(({ value, label }) => (
                    <SelectItem key={value} value={value}>
                      <span className="flex items-center gap-2">
                        <PaymentMethodIcon type={value} size={16} />
                        {label}
                      </span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="flex gap-4">
          <FormField
            control={form.control}
            name="last4"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormLabel>Последние 4 цифры</FormLabel>
                <FormControl>
                  <Input
                    placeholder="1234"
                    inputMode="numeric"
                    maxLength={4}
                    {...field}
                    value={field.value ?? ''}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="balance"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormLabel>Баланс</FormLabel>
                <FormControl>
                  <Input
                    placeholder="1000.0000"
                    inputMode="decimal"
                    {...field}
                    value={field.value ?? ''}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <Button type="submit" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? 'Создание...' : 'Создать метод оплаты'}
        </Button>
      </form>
    </Form>
  )
}
