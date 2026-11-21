const money = new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const formatMoney = (v: number | null | undefined) => (v == null ? '-' : money.format(v))

/** "09:00:00" -> "09:00" */
export const formatTime = (v: string | null | undefined) => (v ? v.slice(0, 5) : '-')

export const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
]

/** Empty form strings become null. */
export const orNull = (v: string) => (v.trim() === '' ? null : v.trim())
export const numOrNull = (v: string) => (v.trim() === '' ? null : Number(v))

export const label = (v: string | null | undefined) => (v ? v.replace(/_/g, ' ') : '-')

/** How each user type is named in the UI; SAAS_STAFF are mindHR's own employees. */
export const USER_TYPE_LABELS: Record<string, string> = {
  SUPER_ADMIN: 'Super Admin',
  ADMIN: 'Admin',
  SAAS_STAFF: 'mindHR Staff',
  ORG_USER: 'Organization User',
}
export const userTypeLabel = (v: string | null | undefined) => (v ? (USER_TYPE_LABELS[v] ?? label(v)) : '-')

/** Bangladeshi mobile number: 01[3-9] + 8 digits, optionally with +88 / 88 in front (same rule as the backend). */
export const MOBILE_NUMBER_RE = /^(\+?88)?01[3-9]\d{8}$/
/** Removes spaces, dashes and brackets people type, e.g. "017-1234 5678" -> "01712345678". */
export const cleanPhone = (v: string) => v.replace(/[\s\-()]/g, '')
