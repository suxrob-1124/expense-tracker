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
    base: '/api/v1/transactions',
    byId: (id: string) => `/api/v1/transactions/${id}`,
    list: (month?: number, year?: number) =>
      month != null && year != null
        ? `/api/v1/transactions?month=${month}&year=${year}`
        : '/api/v1/transactions',
    latest: (page = 0, size = 10) =>
      `/api/v1/transactions/latest?page=${page}&size=${size}`,
    summary: (month?: number, year?: number) =>
      month != null && year != null
        ? `/api/v1/transactions/summary?month=${month}&year=${year}`
        : '/api/v1/transactions/summary',
  },
} as const
