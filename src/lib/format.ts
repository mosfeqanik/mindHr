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
