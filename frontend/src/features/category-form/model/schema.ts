import { z } from 'zod'
import { CATEGORY_ICONS } from '@/entities/category'

/**
 * Zod schema for the category creation form.
 * - `name`: 1–50 chars, required.
 * - `color`: hex color string matching `/^#[0-9a-fA-F]{6}$/`.
 * - `icon`: one of the values in {@link CATEGORY_ICONS}.
 */
export const categorySchema = z.object({
  name: z.string().min(1, 'Название обязательно').max(50, 'Не более 50 символов'),
  color: z.string().regex(/^#[0-9a-fA-F]{6}$/, 'Укажите цвет в формате #RRGGBB'),
  icon: z.enum(CATEGORY_ICONS as [string, ...string[]], { error: 'Выберите иконку' }),
})

/** Inferred type of a valid category form submission. */
export type CategoryFormData = z.infer<typeof categorySchema>
