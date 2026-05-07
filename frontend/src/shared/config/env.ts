export const env = {
  BACKEND_INTERNAL_URL: process.env.BACKEND_INTERNAL_URL ?? 'http://localhost:8080',
  NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080',
} as const
