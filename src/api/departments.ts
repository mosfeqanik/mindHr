import { crud } from './crud'
import type { Department, DepartmentRequest } from './types'

export const departmentsApi = crud<Department, DepartmentRequest>('/api/departments')
