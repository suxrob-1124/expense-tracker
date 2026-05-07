'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'

export async function deleteCategoryAction(id: string): Promise<void> {
  await backendFetch(API.categories.byId(id), {
    method: 'DELETE',
    forwardAccessToken: true,
  })
  revalidatePath('/categories')
}
