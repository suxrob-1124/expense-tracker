'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { parseProblemDetail, formatProblemMessage, type ProblemDetail } from '@/shared/api/problem'
import { API } from '@/shared/api/endpoints'
import type { TransactionResponse } from '@/shared/api/dto'
import { transactionSchema, type TransactionFormValues } from '../model/schema'

/**
 * Result type for create/update server actions.
 *
 * - `{ ok: true; data: TransactionResponse }` — operation succeeded
 * - `{ ok: false; message: string }` — validation error or backend failure
 */
export type TransactionActionResult =
  | { ok: false; message: string }
  | { ok: true; data: TransactionResponse }

/**
 * Result type for the delete server action.
 *
 * - `{ ok: true }` — transaction deleted successfully
 * - `{ ok: false; message: string }` — backend error
 */
export type DeleteActionResult = { ok: false; message: string } | { ok: true }

async function handleResponse(res: Response): Promise<TransactionActionResult> {
  if (!res.ok) {
    const problem: ProblemDetail | null = await parseProblemDetail(res)
    return { ok: false, message: problem ? formatProblemMessage(problem) : `Ошибка ${res.status}` }
  }
  const data: TransactionResponse = await res.json()
  return { ok: true, data }
}

/**
 * Server Action — creates a new transaction.
 *
 * Validates the form values against {@link transactionSchema} before sending a
 * `POST /api/v1/transactions` request. On success, revalidates `/transactions`.
 *
 * @param raw - raw form values from react-hook-form
 * @returns {@link TransactionActionResult}
 */
export async function createTransactionAction(
  raw: TransactionFormValues,
): Promise<TransactionActionResult> {
  const parsed = transactionSchema.safeParse(raw)
  if (!parsed.success) {
    return { ok: false, message: parsed.error.issues[0]?.message ?? 'Ошибка валидации' }
  }

  let res: Response
  try {
    res = await backendFetch(API.transactions.base, {
      method: 'POST',
      body: JSON.stringify(parsed.data),
      forwardAccessToken: true,
    })
  } catch {
    return { ok: false, message: 'Не удалось подключиться к серверу' }
  }

  const result = await handleResponse(res)
  if (result.ok) revalidatePath('/transactions')
  return result
}

/**
 * Server Action — partially updates an existing transaction.
 *
 * Validates the form values and sends a `PATCH /api/v1/transactions/:id` request.
 * On success, revalidates `/transactions`.
 *
 * @param id  - UUID of the transaction to update
 * @param raw - raw form values from react-hook-form
 * @returns {@link TransactionActionResult}
 */
export async function updateTransactionAction(
  id: string,
  raw: TransactionFormValues,
): Promise<TransactionActionResult> {
  const parsed = transactionSchema.safeParse(raw)
  if (!parsed.success) {
    return { ok: false, message: parsed.error.issues[0]?.message ?? 'Ошибка валидации' }
  }

  let res: Response
  try {
    res = await backendFetch(API.transactions.byId(id), {
      method: 'PATCH',
      body: JSON.stringify(parsed.data),
      forwardAccessToken: true,
    })
  } catch {
    return { ok: false, message: 'Не удалось подключиться к серверу' }
  }

  const result = await handleResponse(res)
  if (result.ok) revalidatePath('/transactions')
  return result
}

/**
 * Server Action — deletes a transaction.
 *
 * Sends a `DELETE /api/v1/transactions/:id` request.
 * On success, revalidates `/transactions`.
 *
 * @param id - UUID of the transaction to delete
 * @returns {@link DeleteActionResult}
 */
export async function deleteTransactionAction(id: string): Promise<DeleteActionResult> {
  let res: Response
  try {
    res = await backendFetch(API.transactions.byId(id), {
      method: 'DELETE',
      forwardAccessToken: true,
    })
  } catch {
    return { ok: false, message: 'Не удалось подключиться к серверу' }
  }

  if (!res.ok) {
    const problem: ProblemDetail | null = await parseProblemDetail(res)
    return { ok: false, message: problem ? formatProblemMessage(problem) : `Ошибка ${res.status}` }
  }

  revalidatePath('/transactions')
  return { ok: true }
}
