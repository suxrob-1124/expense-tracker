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
import { CATEGORY_ICONS, ICON_MAP } from '@/entities/category'
import { categorySchema, type CategoryFormData } from '../model/schema'
import { createCategoryAction } from '../api/create-category.action'

export function CategoryCreateForm() {
  const form = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
    defaultValues: { name: '', color: '#6366f1', icon: 'Wallet' },
  })

  const onSubmit = async (values: CategoryFormData) => {
    const result = await createCategoryAction(values)
    if (result.error) {
      toast.error(result.error)
      return
    }
    toast.success('Категория создана')
    form.reset({ name: '', color: values.color, icon: values.icon })
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
                <Input placeholder="Например, Продукты" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="flex gap-4">
          <FormField
            control={form.control}
            name="icon"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormLabel>Иконка</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Выберите иконку" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {CATEGORY_ICONS.map((name) => {
                      const Icon = ICON_MAP[name]
                      return (
                        <SelectItem key={name} value={name}>
                          <span className="flex items-center gap-2">
                            {Icon && <Icon size={16} />}
                            {name}
                          </span>
                        </SelectItem>
                      )
                    })}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="color"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Цвет</FormLabel>
                <FormControl>
                  <input
                    type="color"
                    {...field}
                    className="h-10 w-16 cursor-pointer rounded-md border border-input p-1"
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <Button type="submit" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? 'Создание...' : 'Создать категорию'}
        </Button>
      </form>
    </Form>
  )
}
