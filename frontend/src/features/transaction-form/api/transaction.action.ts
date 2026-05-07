'use server'

import { revalidatePath } from 'next/cache'
import { backendFetch } from '@/shared/api/http'
import { parseProblemDetail, formatProblemMessage, type ProblemDetail } from '@/shared/api/problem'
import { API } from '@/shared/api/endpoints'
import type { TransactionResponse } from '@/shared/api/dto'
import { transactionSchema, type TransactionFormValues } from '../model/schema'

export type TransactionActionResult =
  | { ok: false; message: string }
  | { ok: true; data: TransactionResponse }

export type DeleteActionResult = { ok: false; message: string } | { ok: true }

async function handleResponse(res: Response): Promise<TransactionActionResult> {
  if (!res.ok) {
    const problem: ProblemDetail | null = await parseProblemDetail(res)
    return { ok: false, message: problem ? formatProblemMessage(problem) : `Ошибка ${res.status}` }
  }
  const data: TransactionResponse = await res.json()
  return { ok: true, data }
}

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
