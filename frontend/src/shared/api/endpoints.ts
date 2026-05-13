/**
 * Centralised map of backend API endpoints.
 * Always import from this module — never hardcode URL strings.
 *
 * @example
 * import { API } from '@/shared/api/endpoints'
 * backendFetch(API.transactions.base, ...)
 */
export const API = {
  auth: {
    login: '/api/v1/auth/login',
    refresh: '/api/v1/auth/refresh',
    logout: '/api/v1/auth/logout',
  },
  users: {
    register: '/api/v1/users/register',
    me: '/api/v1/users/me',
    password: '/api/v1/users/me/password',
  },
  categories: {
    base: '/api/v1/categories',
    byId: (id: string) => `/api/v1/categories/${id}`,
  },
  transactions: {
    /** Base path for the transactions resource: `POST /api/v1/transactions` */
    base: '/api/v1/transactions',
    /** Path for a single transaction: `GET | PATCH | DELETE /api/v1/transactions/:id` */
    byId: (id: string) => `/api/v1/transactions/${id}`,
    /**
     * List transactions filtered by month/year.
     * When both parameters are omitted the backend defaults to the current UTC month.
     *
     * @param month - month number 1–12 (optional)
     * @param year  - calendar year (optional)
     */
    list: (month?: number, year?: number) =>
      month != null && year != null
        ? `/api/v1/transactions?month=${month}&year=${year}`
        : '/api/v1/transactions',
    /**
     * Paginated list of the most recent transactions.
     *
     * @param page - zero-based page number (default 0)
     * @param size - page size 1–50 (default 10)
     */
    latest: (page = 0, size = 10) =>
      `/api/v1/transactions/latest?page=${page}&size=${size}`,
    /**
     * Monthly income/expense/balance summary.
     * Omit parameters to default to the current UTC month.
     *
     * @param month - month number 1–12 (optional)
     * @param year  - calendar year (optional)
     */
    summary: (month?: number, year?: number) =>
      month != null && year != null
        ? `/api/v1/transactions/summary?month=${month}&year=${year}`
        : '/api/v1/transactions/summary',
  },
} as const
