// Mirrors backend DTO records (LoginRequest/Response, EmployeeRequest/Response).
export interface LoginRequest {
  email: string
  password: string
}

export type UserType = 'SUPER_ADMIN' | 'SAAS_STAFF' | 'ORG_USER'

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  fullName: string
  userType: UserType
}

export const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN', 'PROBATION'] as const
export type EmploymentType = (typeof EMPLOYMENT_TYPES)[number]

export interface Employee {
  id: number
  organizationId: number
  userId: number | null
  employeeCode: string
  fullName: string
  email: string | null
  phone: string | null
  position: string | null
  nationalIdSerial: string | null
  photoUrl: string | null
  address: string | null
  emergencyContactName: string | null
  emergencyContactPhone: string | null
  emergencyContactRelation: string | null
  branchId: number | null
  departmentId: number | null
  managerId: number | null
  employmentType: EmploymentType | null
  joinedAt: string | null
  active: boolean
}

export type EmployeeRequest = Partial<Omit<Employee, 'id'>> & {
  fullName: string
  password?: string
}

export interface Branch {
  id: number
  organizationId: number
  name: string
  code: string | null
  address: string | null
  phone: string | null
  managerId: number | null
  active: boolean
  latitude: number | null
  longitude: number | null
  geofenceRadiusM: number | null
}

export type BranchRequest = Partial<Omit<Branch, 'id'>> & { name: string }

export interface Department {
  id: number
  organizationId: number
  name: string
  branchId: number | null
  headId: number | null
  active: boolean
}

export type DepartmentRequest = Partial<Omit<Department, 'id'>> & { name: string }

export const ATTENDANCE_STATUSES = ['PRESENT', 'LATE', 'ABSENT', 'HALF_DAY', 'ON_LEAVE'] as const
export type AttendanceStatus = (typeof ATTENDANCE_STATUSES)[number]

export const ATTENDANCE_METHODS = ['MOBILE_GPS', 'SELFIE', 'QR', 'PIN', 'MANUAL', 'BIOMETRIC'] as const
export type AttendanceMethod = (typeof ATTENDANCE_METHODS)[number]

export interface Attendance {
  id: number
  organizationId: number
  employeeId: number
  workDate: string
  checkInAt: string | null
  checkOutAt: string | null
  checkInLatitude: number | null
  checkInLongitude: number | null
  checkOutLatitude: number | null
  checkOutLongitude: number | null
  method: AttendanceMethod | null
  status: AttendanceStatus | null
  lateMinutes: number
  workedMinutes: number
  note: string | null
}

export type AttendanceRequest = Partial<Omit<Attendance, 'id'>> & { employeeId: number; workDate: string }

// ---- Leave ----
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface LeaveType {
  id: number
  organizationId: number
  name: string
  annualDays: number
  paid: boolean
  active: boolean
}
export type LeaveTypeRequest = Partial<Omit<LeaveType, 'id'>> & { name: string }

export interface LeaveBalance {
  id: number
  organizationId: number
  employeeId: number
  leaveTypeId: number
  year: number
  entitledDays: number
  usedDays: number
}
export type LeaveBalanceRequest = Partial<Omit<LeaveBalance, 'id'>> & {
  employeeId: number
  leaveTypeId: number
  entitledDays: number
}

export interface LeaveRequest {
  id: number
  organizationId: number
  employeeId: number
  leaveTypeId: number
  fromDate: string
  toDate: string
  days: number
  reason: string | null
  status: LeaveStatus
  decidedById: number | null
  decidedAt: string | null
  decisionNote: string | null
}
export interface LeaveApplyRequest {
  leaveTypeId: number
  fromDate: string
  toDate: string
  reason?: string | null
}

// ---- Shifts ----
export interface Shift {
  id: number
  organizationId: number
  name: string
  startTime: string
  endTime: string
  active: boolean
}
export type ShiftRequest = Partial<Omit<Shift, 'id'>> & { name: string; startTime: string; endTime: string }

export interface ShiftAssignment {
  id: number
  organizationId: number
  employeeId: number
  shiftId: number
  workDate: string
}
export type ShiftAssignmentRequest = Partial<Omit<ShiftAssignment, 'id'>> & {
  employeeId: number
  shiftId: number
  workDate: string
}

// ---- Platform (SaaS side) ----
export const ORGANIZATION_STATUSES = ['TRIAL', 'ACTIVE', 'EXPIRED', 'SUSPENDED'] as const
export type OrganizationStatus = (typeof ORGANIZATION_STATUSES)[number]

export interface Organization {
  id: number
  name: string
  slug: string
  status: OrganizationStatus
  email: string | null
  phone: string | null
  address: string | null
  trialEndsAt: string | null
  activatedAt: string | null
  ownerUserId: number | null
  ownerName: string | null
  ownerEmail: string | null
}
/** The backend generates the slug from the name on create and keeps it on update. */
export type OrganizationRequest = Partial<Omit<Organization, 'id' | 'slug' | 'activatedAt' | 'ownerUserId' | 'ownerName' | 'ownerEmail'>> & {
  name: string
  status: OrganizationStatus
  /** Required on create: the plan of the organization's first subscription. */
  planId?: number
}

export interface User {
  id: number
  organizationId: number | null
  email: string
  fullName: string
  phone: string | null
  userType: UserType
  active: boolean
  lastLoginAt: string | null
  roles: { id: number; name: string }[]
}
export interface UserRequest {
  organizationId?: number | null
  email: string
  password?: string | null
  fullName: string
  phone?: string | null
  userType: UserType
  active?: boolean
  roleIds?: number[]
}

// ---- Assets and documents ----
export const ASSET_STATUSES = ['AVAILABLE', 'ASSIGNED', 'MAINTENANCE', 'RETIRED'] as const
export type AssetStatus = (typeof ASSET_STATUSES)[number]

export interface Asset {
  id: number
  organizationId: number
  name: string
  assetTag: string
  serialNumber: string | null
  category: string | null
  description: string | null
  status: AssetStatus
}
export type AssetRequest = Partial<Omit<Asset, 'id'>> & { name: string; assetTag: string }

export interface AssetAssignment {
  id: number
  organizationId: number
  assetId: number
  employeeId: number
  assignedAt: string
  returnedAt: string | null
  conditionNote: string | null
}
export interface AssetAssignRequest {
  assetId: number
  employeeId: number
  assignedAt?: string | null
}
export interface AssetReturnRequest {
  returnedAt?: string | null
  conditionNote?: string | null
  assetStatus?: Exclude<AssetStatus, 'ASSIGNED'>
}

export const DOCUMENT_TYPES = [
  'NOC',
  'EXPERIENCE_LETTER',
  'SALARY_CERTIFICATE',
  'PAY_SLIP',
  'OFFER_LETTER',
  'APPOINTMENT_LETTER',
  'INCREMENT_LETTER',
  'PROMOTION_LETTER',
  'OTHER',
] as const
export type DocumentType = (typeof DOCUMENT_TYPES)[number]

export interface EmployeeDocument {
  id: number
  organizationId: number
  employeeId: number
  type: DocumentType
  title: string
  fileUrl: string
  issuedAt: string | null
  note: string | null
}
export type EmployeeDocumentRequest = Partial<Omit<EmployeeDocument, 'id'>> & {
  employeeId: number
  type: DocumentType
  title: string
  fileUrl: string
}

// ---- Payroll ----
export type PayrollStatus = 'DRAFT' | 'FINALIZED' | 'PAID'

export interface PayrollRun {
  id: number
  organizationId: number
  year: number
  month: number
  branchId: number | null
  status: PayrollStatus
  note: string | null
}
export interface PayrollRunCreateRequest {
  organizationId?: number
  year: number
  month: number
  branchId?: number | null
  note?: string | null
}

export interface Payslip {
  id: number
  organizationId: number
  payrollRunId: number
  employeeId: number
  basicSalary: number
  allowances: number
  overtime: number
  bonus: number
  commission: number
  grossSalary: number
  lateDeduction: number
  absentDeduction: number
  loanDeduction: number
  advanceDeduction: number
  netSalary: number
  daysPresent: number
  daysAbsent: number
  daysLate: number
  status: PayrollStatus
}
export interface PayslipAdjustRequest {
  overtime?: number | null
  bonus?: number | null
  commission?: number | null
  loanDeduction?: number | null
  advanceDeduction?: number | null
}

// ---- Platform: subscriptions, billing, contracts, support, settings ----
export const BILLING_CYCLES = ['MONTHLY', 'YEARLY'] as const
export type BillingCycle = (typeof BILLING_CYCLES)[number]

export const PLAN_FEATURES = [
  'ATTENDANCE',
  'LEAVE',
  'SHIFTS',
  'PAYROLL',
  'REPORTS',
  'ASSETS',
  'PERFORMANCE',
  'MOBILE_APP',
  'MULTI_BRANCH',
  'ADVANCED_PERMISSIONS',
] as const
export type PlanFeature = (typeof PLAN_FEATURES)[number]
/** GET /api/subscriptions/current: empty features when the organization has no usable subscription. */
export interface CurrentPlan {
  planId: number | null
  planName: string | null
  status: SubscriptionStatus | null
  features: PlanFeature[]
}

export interface SubscriptionPlan {
  id: number
  name: string
  description: string | null
  price: number
  currency: string | null
  billingCycle: BillingCycle
  maxEmployees: number | null
  maxBranches: number | null
  features: PlanFeature[]
  trialDays: number
  active: boolean
}
export type SubscriptionPlanRequest = Omit<SubscriptionPlan, 'id'>

export const SUBSCRIPTION_STATUSES = [
  'TRIAL',
  'ACTIVE',
  'PAYMENT_FAILED',
  'GRACE_PERIOD',
  'CANCELLED',
  'EXPIRED',
  'SUSPENDED',
] as const
export type SubscriptionStatus = (typeof SUBSCRIPTION_STATUSES)[number]

export interface Subscription {
  id: number
  organizationId: number
  planId: number
  planName: string
  status: SubscriptionStatus
  startDate: string
  endDate: string | null
  renewalDate: string | null
  trialEndsAt: string | null
  autoRenew: boolean
  cancelledAt: string | null
}
export interface SubscriptionRequest {
  organizationId?: number
  planId: number
  status: SubscriptionStatus
  startDate: string
  endDate?: string | null
  renewalDate?: string | null
  trialEndsAt?: string | null
  autoRenew?: boolean
}
export interface SubscriptionUpdateRequest {
  endDate?: string | null
  renewalDate?: string | null
  trialEndsAt?: string | null
  autoRenew?: boolean
}
export interface SubscriptionHistory {
  id: number
  subscriptionId: number
  fromPlanName: string | null
  toPlanName: string | null
  fromStatus: SubscriptionStatus | null
  toStatus: SubscriptionStatus | null
  changedByName: string | null
  note: string | null
  changedAt: string
}

export const INVOICE_STATUSES = ['PENDING', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED'] as const
export type InvoiceStatus = (typeof INVOICE_STATUSES)[number]

export interface Invoice {
  id: number
  organizationId: number
  subscriptionId: number | null
  invoiceNumber: string
  amount: number
  currency: string | null
  status: InvoiceStatus
  issueDate: string
  dueDate: string
  paidAt: string | null
  notes: string | null
}
export interface InvoiceRequest {
  organizationId?: number
  subscriptionId?: number | null
  invoiceNumber: string
  amount: number
  currency?: string | null
  issueDate: string
  dueDate: string
  notes?: string | null
}

export const PAYMENT_STATUSES = ['PENDING', 'CONFIRMED', 'FAILED', 'REFUNDED'] as const
export type PaymentStatus = (typeof PAYMENT_STATUSES)[number]

export interface Payment {
  id: number
  organizationId: number
  invoiceId: number
  paymentMethodId: number | null
  amount: number
  currency: string | null
  status: PaymentStatus
  gateway: string | null
  gatewayTransactionId: string | null
  paidAt: string | null
  hasReceipt: boolean
}
export interface PaymentRequest {
  organizationId?: number
  invoiceId: number
  paymentMethodId?: number | null
  amount: number
  currency?: string | null
  gateway?: string | null
  gatewayTransactionId?: string | null
}
export interface Receipt {
  id: number
  paymentId: number
  receiptNumber: string
  amount: number
  issuedAt: string
}

export const CONTRACT_STATUSES = ['PENDING', 'ACTIVE', 'EXPIRED', 'TERMINATED'] as const
export type ContractStatus = (typeof CONTRACT_STATUSES)[number]

export interface Contract {
  id: number
  organizationId: number
  title: string
  counterpartyName: string | null
  status: ContractStatus
  startDate: string | null
  endDate: string | null
  value: number | null
  currency: string | null
  terms: string | null
}
export type ContractRequest = Partial<Omit<Contract, 'id' | 'status'>> & { title: string }
export interface ContractHistory {
  id: number
  contractId: number
  fromStatus: ContractStatus | null
  toStatus: ContractStatus
  changedByName: string | null
  note: string | null
  changedAt: string
}

export const TICKET_TYPES = ['REQUEST', 'COMPLAINT'] as const
export type TicketType = (typeof TICKET_TYPES)[number]
export const TICKET_STATUSES = ['OPEN', 'IN_PROGRESS', 'WAITING_FOR_CLIENT', 'RESOLVED', 'CLOSED'] as const
export type TicketStatus = (typeof TICKET_STATUSES)[number]
export const TICKET_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const
export type TicketPriority = (typeof TICKET_PRIORITIES)[number]

export interface SupportTicket {
  id: number
  organizationId: number
  createdByUserId: number | null
  createdByName: string | null
  assignedToUserId: number | null
  assignedToName: string | null
  ticketNumber: string
  subject: string
  description: string
  type: TicketType
  status: TicketStatus
  priority: TicketPriority
  resolvedAt: string | null
  closedAt: string | null
}
export interface SupportTicketRequest {
  organizationId?: number
  subject: string
  description: string
  type: TicketType
  priority: TicketPriority
}
export interface TicketComment {
  id: number
  ticketId: number
  authorId: number | null
  authorName: string | null
  body: string
  internal: boolean
}

export interface SystemSetting {
  id: number
  key: string
  value: string | null
  description: string | null
}
export type SystemSettingRequest = Omit<SystemSetting, 'id'>
