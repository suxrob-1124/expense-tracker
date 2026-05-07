import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { CategoryResponse } from '@/shared/api/dto'
import { CategoryCard } from '@/entities/category'
import { CategoryCreateForm, deleteCategoryAction } from '@/features/category-form'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'

export async function CategoriesView() {
  const res = await backendFetch(API.categories.base, { forwardAccessToken: true })
  const categories: CategoryResponse[] = res.ok ? await res.json() : []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Категории</h1>
        <p className="text-sm text-muted-foreground mt-0.5">Управление категориями</p>
      </div>

      {categories.length === 0 ? (
        <p className="text-muted-foreground text-center py-8">
          Категорий пока нет — создайте первую ниже
        </p>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {categories.map((cat) => (
            <CategoryCard key={cat.id} category={cat} onDelete={deleteCategoryAction} />
          ))}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Новая категория</CardTitle>
        </CardHeader>
        <CardContent>
          <CategoryCreateForm />
        </CardContent>
      </Card>
    </div>
  )
}
