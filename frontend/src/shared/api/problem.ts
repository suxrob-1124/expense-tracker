export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail?: string
  instance?: string
}

export async function parseProblemDetail(res: Response): Promise<ProblemDetail | null> {
  const ct = res.headers.get('content-type') ?? ''
  if (!ct.includes('application/problem+json')) return null
  try {
    return (await res.json()) as ProblemDetail
  } catch {
    return null
  }
}

const BACKEND_MESSAGES: Record<string, string> = {
  'Invalid credentials': 'Неверный email или пароль',
  'User already exists': 'Пользователь с таким email уже зарегистрирован',
  'Account is locked': 'Аккаунт временно заблокирован. Попробуйте позже',
  'Bad credentials': 'Неверный email или пароль',
}

export function formatProblemMessage(problem: ProblemDetail): string {
  const raw = problem.detail ?? problem.title ?? ''
  return BACKEND_MESSAGES[raw] ?? (raw || 'Произошла ошибка')
}
