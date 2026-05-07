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
} as const
