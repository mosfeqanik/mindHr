import type { ActivityLog } from '../api/types'

const VERBS: Record<string, string> = {
  CREATE: 'Created',
  UPDATE: 'Updated',
  DELETE: 'Deleted',
  APPROVE: 'Approved',
  REJECT: 'Rejected',
  CANCEL: 'Cancelled',
  STATUS: 'Changed status of',
  PLAN: 'Changed plan of',
  CHECK_IN: 'Checked in',
  CHECK_OUT: 'Checked out',
  CHANGE_PASSWORD: 'Changed password',
  FINALIZE: 'Finalized',
  PAY: 'Paid',
  RETURN: 'Returned',
  RECEIPT: 'Issued receipt for',
  COMMENTS: 'Commented on',
  ASSIGN: 'Assigned',
  CONVERT: 'Converted',
  OWNER: 'Changed owner of',
  ADJUSTMENTS: 'Adjusted',
  SENT: 'Sent',
}

const titleCase = (s: string) => s.toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase())

/** "Approved leave request #7", "Checked in", "Created employee #12". */
export function describeActivity(a: ActivityLog): string {
  const verb = VERBS[a.action] ?? titleCase(a.action)
  if (a.entityType === 'ACCOUNT' || (a.entityType === 'ATTENDANCE' && a.action.startsWith('CHECK_'))) return verb
  const target = titleCase(a.entityType).toLowerCase()
  return `${verb} ${target}${a.entityId != null ? ` #${a.entityId}` : ''}`
}

export function activityBadge(action: string): string {
  if (action === 'CREATE') return 'bg-success'
  if (action === 'DELETE' || action === 'REJECT' || action === 'CANCEL') return 'bg-danger'
  if (action === 'UPDATE') return 'bg-info'
  return 'bg-secondary'
}

/** "2026-11-08T18:55:58.7" -> "2026-11-08 18:55" */
export const formatDateTime = (v: string | null | undefined) => (v ? v.slice(0, 16).replace('T', ' ') : '-')

/** Who may open the activity log: platform users, and org users with organization-wide data scope (backend enforces it). */
export const canViewActivity = (userType?: string, dataScope?: string) =>
  userType === 'SUPER_ADMIN' || userType === 'SAAS_STAFF' || dataScope === 'ORGANIZATION'
