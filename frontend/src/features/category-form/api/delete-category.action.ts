'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'

/**
 * Server Action — deletes a category by UUID.
 *
 * Calls `DELETE /api/v1/categories/:id`. Revalidates `/categories` on success.
 *
 * @param id - UUID of the category to delete.
 * @returns `{}` on success or `{ error: string }` if the request fails.
 */
export async function deleteCategoryAction(id: string): Promise<{ error?: string }> {
  const res = await backendFetch(API.categories.byId(id), {
    method: 'DELETE',
    forwardAccessToken: true,
  })

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    return { error: problem.detail ?? problem.title ?? 'Ошибка удаления категории' }
  }

  revalidatePath('/categories')
  return {}
}
