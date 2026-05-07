'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { CategoryFormData } from '../model/schema'

export async function createCategoryAction(data: CategoryFormData): Promise<{ error?: string }> {
  const res = await backendFetch(API.categories.base, {
    method: 'POST',
    body: JSON.stringify(data),
    headers: { 'Content-Type': 'application/json' },
    forwardAccessToken: true,
  })

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    return { error: problem.detail ?? problem.title ?? 'Ошибка создания категории' }
  }

  revalidatePath('/categories')
  return {}
}
