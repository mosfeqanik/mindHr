import axios from 'axios'
import { withActiveOrg } from './orgScope'

export const TOKEN_KEY = 'mindhr.token'

// Empty base URL = same-origin "/api" (Vite proxy in dev, nginx in Docker).
export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? '' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return withActiveOrg(config)
})

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const isLogin = error.config?.url?.includes('/api/auth/login')
    if (error.response?.status === 401 && !isLogin) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

/** Best-effort message from the backend's error body (GlobalExceptionHandler) or axios. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined
    return data?.message ?? data?.error ?? error.message
  }
  return error instanceof Error ? error.message : 'Unexpected error'
}
