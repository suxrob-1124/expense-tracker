'use client'

import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/shared/ui/form'
import { registerSchema, type RegisterFormValues } from '../model/schema'
import { registerAction } from '../api/register.action'

/**
 * Client Component that renders the full registration form
 * (firstName, lastName, email, password, acceptTerms checkbox).
 *
 * On submit, calls {@link registerAction}. A successful registration auto-logs
 * the user in and redirects to `/transactions`. Errors are shown via toast.
 */
export function RegisterForm() {
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '', firstName: '', lastName: '', acceptTerms: false },
  })

  const onSubmit = async (values: RegisterFormValues) => {
    const result = await registerAction(values)
    if (!result.ok) {
      toast.error(result.message)
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="firstName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Имя</FormLabel>
                <FormControl>
                  <Input placeholder="Иван" autoComplete="given-name" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="lastName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Фамилия</FormLabel>
                <FormControl>
                  <Input placeholder="Иванов" autoComplete="family-name" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input type="email" placeholder="you@example.com" autoComplete="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Пароль</FormLabel>
              <FormControl>
                <Input type="password" placeholder="Минимум 12 символов" autoComplete="new-password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="acceptTerms"
          render={({ field }) => (
            <FormItem>
              <div className="flex items-start gap-2">
                <FormControl>
                  <input
                    type="checkbox"
                    id="acceptTerms"
                    checked={field.value}
                    onChange={field.onChange}
                    className="mt-0.5 h-4 w-4 shrink-0 cursor-pointer rounded border border-input accent-primary"
                  />
                </FormControl>
                <FormLabel htmlFor="acceptTerms" className="cursor-pointer font-normal leading-snug">
                  Я принимаю{' '}
                  <Link href="/terms" className="font-medium text-primary underline underline-offset-2 hover:opacity-70 transition-opacity">
                    пользовательское соглашение
                  </Link>{' '}
                  и{' '}
                  <Link href="/privacy" className="font-medium text-primary underline underline-offset-2 hover:opacity-70 transition-opacity">
                    политику обработки данных
                  </Link>
                </FormLabel>
              </div>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? 'Регистрация...' : 'Создать аккаунт'}
        </Button>
      </form>
    </Form>
  )
}
