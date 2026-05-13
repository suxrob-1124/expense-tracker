'use client'

import { toast } from 'sonner'
import { ICON_MAP } from '../model/icons'
import type { CategoryResponse } from '../model/types'
import { Trash2 } from 'lucide-react'

/** Props for the {@link CategoryCard} component. */
interface CategoryCardProps {
  /** The category data to display. */
  category: CategoryResponse
  /**
   * Called when the user confirms deletion.
   * @param id - UUID of the category to delete.
   * @returns An object with an optional `error` message on failure.
   */
  onDelete: (id: string) => Promise<{ error?: string }>
}

/**
 * Client Component that renders a single category card.
 *
 * Displays the category icon (from {@link ICON_MAP}) tinted with the category color,
 * the category name, and a delete button that appears on hover.
 * Prompts for confirmation before calling {@link onDelete}.
 */
export function CategoryCard({ category, onDelete }: CategoryCardProps) {
  const Icon = ICON_MAP[category.icon]

  async function handleDelete() {
    if (!window.confirm(`Удалить категорию «${category.name}»?`)) return
    const result = await onDelete(category.id)
    if (result.error) toast.error(result.error)
  }

  return (
    <div className="flex flex-col items-center gap-2 rounded-xl border p-4 relative group">
      <div
        className="flex h-12 w-12 items-center justify-center rounded-full"
        style={{ backgroundColor: category.color + '33' }}
      >
        {Icon ? (
          <Icon size={22} style={{ color: category.color }} />
        ) : (
          <span className="text-lg">{category.name.charAt(0)}</span>
        )}
      </div>
      <span className="text-sm font-medium text-center leading-tight">{category.name}</span>
      <button
        onClick={handleDelete}
        className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-destructive"
        aria-label={`Удалить ${category.name}`}
      >
        <Trash2 size={14} />
      </button>
    </div>
  )
}
