import { backendFetch } from '@/shared/api/http'
import { API } from '@/shared/api/endpoints'
import type { PaymentMethodResponse } from '@/entities/payment-method'
import { PaymentMethodForm } from '@/features/payment-method-form'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { PaymentMethodsList } from './PaymentMethodsList'

/**
 * Server Component that renders the payment methods management page.
 *
 * Fetches the user's payment methods via `GET /api/v1/payment-methods` server-side
 * and hands them to {@link PaymentMethodsList}, which filters archived items in the
 * client. Renders the {@link PaymentMethodForm} below for adding new methods.
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

      <PaymentMethodsList paymentMethods={paymentMethods} />

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
