import { api } from './client'
import type { PayrollRun, PayrollRunCreateRequest, Payslip, PayslipAdjustRequest } from './types'

export const payrollApi = {
  listRuns: () => api.get<PayrollRun[]>('/api/payroll-runs').then((r) => r.data),
  getRun: (id: number) => api.get<PayrollRun>(`/api/payroll-runs/${id}`).then((r) => r.data),
  generate: (body: PayrollRunCreateRequest) => api.post<PayrollRun>('/api/payroll-runs', body).then((r) => r.data),
  finalize: (id: number) => api.post<PayrollRun>(`/api/payroll-runs/${id}/finalize`).then((r) => r.data),
  markPaid: (id: number) => api.post<PayrollRun>(`/api/payroll-runs/${id}/pay`).then((r) => r.data),
  deleteDraft: (id: number) => api.delete(`/api/payroll-runs/${id}`).then(() => undefined),
  listPayslips: (params?: Record<string, unknown>) =>
    api.get<Payslip[]>('/api/payslips', { params }).then((r) => r.data),
  adjust: (id: number, body: PayslipAdjustRequest) =>
    api.put<Payslip>(`/api/payslips/${id}/adjustments`, body).then((r) => r.data),
}
