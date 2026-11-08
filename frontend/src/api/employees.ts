import { api } from './client'
import type { Employee, EmployeeRequest } from './types'

export const employeesApi = {
  list: (organizationId?: number) =>
    api.get<Employee[]>('/api/employees', { params: { organizationId } }).then((r) => r.data),
  get: (id: number) => api.get<Employee>(`/api/employees/${id}`).then((r) => r.data),
  create: (body: EmployeeRequest) => api.post<Employee>('/api/employees', body).then((r) => r.data),
  update: (id: number, body: EmployeeRequest) =>
    api.put<Employee>(`/api/employees/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/employees/${id}`).then(() => undefined),
}
