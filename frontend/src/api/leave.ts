import { api } from './client'
import { crud } from './crud'
import type {
  LeaveApplyRequest,
  LeaveBalance,
  LeaveBalanceRequest,
  LeaveRequest,
  LeaveType,
  LeaveTypeRequest,
} from './types'

export const leaveTypesApi = crud<LeaveType, LeaveTypeRequest>('/api/leave-types')
export const leaveBalancesApi = crud<LeaveBalance, LeaveBalanceRequest>('/api/leave-balances')

const requests = '/api/leave-requests'
export const leaveRequestsApi = {
  list: (params?: Record<string, unknown>) => api.get<LeaveRequest[]>(requests, { params }).then((r) => r.data),
  apply: (body: LeaveApplyRequest) => api.post<LeaveRequest>(requests, body).then((r) => r.data),
  approve: (id: number, note?: string | null) =>
    api.post<LeaveRequest>(`${requests}/${id}/approve`, { note }).then((r) => r.data),
  reject: (id: number, note?: string | null) =>
    api.post<LeaveRequest>(`${requests}/${id}/reject`, { note }).then((r) => r.data),
  cancel: (id: number) => api.post<LeaveRequest>(`${requests}/${id}/cancel`).then((r) => r.data),
}
