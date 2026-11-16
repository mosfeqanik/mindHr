import { api } from './client'
import { crud } from './crud'
import type {
  Contract,
  CurrentPlan,
  ContractHistory,
  ContractRequest,
  ContractStatus,
  Invoice,
  InvoiceRequest,
  InvoiceStatus,
  Organization,
  OrganizationRequest,
  Payment,
  PaymentRequest,
  PaymentStatus,
  Receipt,
  Subscription,
  SubscriptionHistory,
  SubscriptionPlan,
  SubscriptionPlanRequest,
  SubscriptionRequest,
  SubscriptionStatus,
  SubscriptionUpdateRequest,
  SupportTicket,
  SupportTicketRequest,
  SystemSetting,
  SystemSettingRequest,
  TicketComment,
  TicketStatus,
  User,
  UserRequest,
} from './types'

export const organizationsApi = {
  ...crud<Organization, OrganizationRequest>('/api/organizations'),
  setOwner: (id: number, ownerUserId: number) =>
    api.patch<Organization>(`/api/organizations/${id}/owner`, { ownerUserId }).then((r) => r.data),
}

export const usersApi = crud<User, UserRequest>('/api/users')

export const subscriptionPlansApi = crud<SubscriptionPlan, SubscriptionPlanRequest>('/api/subscription-plans')

// The org-scoped resources below get organizationId from the organization picker (see orgScope.ts).
const subs = '/api/subscriptions'
export const subscriptionsApi = {
  list: () => api.get<Subscription[]>(subs).then((r) => r.data),
  current: () => api.get<CurrentPlan>(`${subs}/current`).then((r) => r.data),
  create: (body: SubscriptionRequest) => api.post<Subscription>(subs, body).then((r) => r.data),
  update: (id: number, body: SubscriptionUpdateRequest) =>
    api.put<Subscription>(`${subs}/${id}`, body).then((r) => r.data),
  history: (id: number) => api.get<SubscriptionHistory[]>(`${subs}/${id}/history`).then((r) => r.data),
  changeStatus: (id: number, status: SubscriptionStatus, note?: string | null) =>
    api.patch<Subscription>(`${subs}/${id}/status`, { status, note }).then((r) => r.data),
  changePlan: (id: number, planId: number, note?: string | null) =>
    api.patch<Subscription>(`${subs}/${id}/plan`, { planId, note }).then((r) => r.data),
}

export const invoicesApi = {
  ...crud<Invoice, InvoiceRequest>('/api/invoices'),
  changeStatus: (id: number, status: InvoiceStatus) =>
    api.patch<Invoice>(`/api/invoices/${id}/status`, { status }).then((r) => r.data),
}

export const paymentsApi = {
  ...crud<Payment, PaymentRequest>('/api/payments'),
  changeStatus: (id: number, status: PaymentStatus) =>
    api.patch<Payment>(`/api/payments/${id}/status`, { status }).then((r) => r.data),
  issueReceipt: (id: number) => api.post<Receipt>(`/api/payments/${id}/receipt`).then((r) => r.data),
}

export const contractsApi = {
  ...crud<Contract, ContractRequest>('/api/contracts'),
  changeStatus: (id: number, status: ContractStatus, note?: string | null) =>
    api.patch<Contract>(`/api/contracts/${id}/status`, { status, note }).then((r) => r.data),
  history: (id: number) => api.get<ContractHistory[]>(`/api/contracts/${id}/history`).then((r) => r.data),
}

const tickets = '/api/support-tickets'
export const supportTicketsApi = {
  ...crud<SupportTicket, SupportTicketRequest>(tickets),
  changeStatus: (id: number, status: TicketStatus) =>
    api.patch<SupportTicket>(`${tickets}/${id}/status`, { status }).then((r) => r.data),
  assign: (id: number, assignedToUserId: number) =>
    api.patch<SupportTicket>(`${tickets}/${id}/assign`, { assignedToUserId }).then((r) => r.data),
  comments: (id: number) => api.get<TicketComment[]>(`${tickets}/${id}/comments`).then((r) => r.data),
  addComment: (id: number, body: string, internal: boolean) =>
    api.post<TicketComment>(`${tickets}/${id}/comments`, { body, internal }).then((r) => r.data),
}

export const systemSettingsApi = crud<SystemSetting, SystemSettingRequest>('/api/system-settings')
