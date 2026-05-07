import { z } from 'zod'
import { CATEGORY_ICONS } from '@/entities/category'

export const categorySchema = z.object({
  name: z.string().min(1, 'Название обязательно').max(50, 'Не более 50 символов'),
  color: z.string().regex(/^#[0-9a-fA-F]{6}$/, 'Укажите цвет в формате #RRGGBB'),
  icon: z.enum(CATEGORY_ICONS as [string, ...string[]], { error: 'Выберите иконку' }),
})

export type CategoryFormData = z.infer<typeof categorySchema>
