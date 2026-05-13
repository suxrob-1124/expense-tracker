/**
 * RFC 7807 Problem Details object returned by the backend on any error response.
 *
 * <p>Mirrors the Spring Boot `ProblemDetail` shape serialised as `application/problem+json`.
 * All error handlers in the backend set `type`, `title`, and `status`; `detail` carries
 * the human-readable explanation.
 *
 * @see https://www.rfc-editor.org/rfc/rfc7807
 */
export interface ProblemDetail {
  /** An absolute URI that identifies the problem type (e.g. `"https://httpstatuses.io/400"`). */
  type: string
  /** Short, human-readable summary of the problem type (e.g. `"Validation Failed"`). */
  title: string
  /** HTTP status code (mirrors the response status, e.g. `400`). */
  status: number
  /** Human-readable explanation specific to this occurrence of the problem. */
  detail?: string
  /** URI reference that identifies the specific occurrence of the problem. */
  instance?: string
}

/**
 * Attempts to parse an RFC 7807 Problem Details body from a backend response.
 *
 * <p>Returns `null` when the response `Content-Type` is not `application/problem+json`
 * or when JSON parsing fails. Callers should fall back to a generic error message in
 * that case.
 *
 * @param res - The `Response` object to parse.
 * @returns The parsed {@link ProblemDetail} or `null` if not applicable.
 */
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

/**
 * Converts a {@link ProblemDetail} into a user-facing error message string.
 *
 * <p>Applies a lookup table to translate well-known backend `detail` / `title` strings
 * into localised Russian messages. Falls back to the raw `detail`, then `title`, then
 * a generic error string when no mapping is found.
 *
 * @param problem - The parsed RFC 7807 problem object.
 * @returns A localised error message suitable for display in the UI.
 *
 * @example
 * const problem = await parseProblemDetail(res)
 * if (problem) toast.error(formatProblemMessage(problem))
 */
export function formatProblemMessage(problem: ProblemDetail): string {
  const raw = problem.detail ?? problem.title ?? ''
  return BACKEND_MESSAGES[raw] ?? (raw || 'Произошла ошибка')
}
