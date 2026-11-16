import { api } from './client'
import type { LoginRequest, LoginResponse } from './types'

export const authApi = {
  login: (body: LoginRequest) => api.post<LoginResponse>('/api/auth/login', body).then((r) => r.data),
}
