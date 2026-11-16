import { api } from './client'

/** Typed REST helpers for the standard /api/<resource> controllers (list, get, create, update, delete). */
export function crud<T, Req>(path: string) {
  return {
    list: (params?: Record<string, unknown>) => api.get<T[]>(path, { params }).then((r) => r.data),
    get: (id: number) => api.get<T>(`${path}/${id}`).then((r) => r.data),
    create: (body: Req) => api.post<T>(path, body).then((r) => r.data),
    update: (id: number, body: Req) => api.put<T>(`${path}/${id}`, body).then((r) => r.data),
    remove: (id: number) => api.delete(`${path}/${id}`).then(() => undefined),
  }
}
