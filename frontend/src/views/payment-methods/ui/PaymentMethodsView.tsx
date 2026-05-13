import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import { PaymentMethodCard, type PaymentMethodResponse } from '@/entities/payment-method'
import {
  PaymentMethodForm,
  deletePaymentMethodAction,
  toggleArchivePaymentMethodAction,
} from '@/features/payment-method-form'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'

/**
 * Server Component that renders the payment methods management page.
 *
 * Fetches the user's payment methods via `GET /api/v1/payment-methods` server-side.
 * Renders a grid of {@link PaymentMethodCard} components (with archive/delete
 * actions) and the {@link PaymentMethodForm} below. Empty state suggests
 * creating the first method.
 */
export async function PaymentMethodsView() {
  const res = await backendFetch(API.paymentMethods.base, { forwardAccessToken: true })
  const paymentMethods: PaymentMethodResponse[] = res.ok ? await res.json() : []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Методы оплаты</h1>
        <p className="text-sm text-muted-foreground mt-0.5">
          Карты, наличные и банковские счета
        </p>
      </div>

      {paymentMethods.length === 0 ? (
        <p className="text-muted-foreground text-center py-8">
          Методов оплаты пока нет — создайте первый ниже
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {paymentMethods.map((pm) => (
            <PaymentMethodCard
              key={pm.id}
              paymentMethod={pm}
              onDelete={deletePaymentMethodAction}
              onToggleArchive={toggleArchivePaymentMethodAction}
            />
          ))}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Новый метод оплаты</CardTitle>
        </CardHeader>
        <CardContent>
          <PaymentMethodForm />
        </CardContent>
      </Card>
    </div>
  )
}
