// Mirrors Java records 1:1
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string    // @Size(min=12, max=128)
  firstName: string   // @Size(max=100)
  lastName: string    // @Size(max=100)
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string // @Size(min=12, max=128)
}

export interface UserResponse {
  id: string          // UUID
  email: string
  firstName: string
  lastName: string
  role: string        // 'USER' | 'ADMIN'
  createdAt: string   // Instant ISO-8601
}

export interface AuthResponse {
  accessToken: string
  tokenType: string   // 'Bearer'
  expiresInSeconds: number
  user: UserResponse
}
