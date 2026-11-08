import type { PayrollStatus } from '../../api/types'

const CLASSES: Record<PayrollStatus, string> = {
  DRAFT: 'bg-secondary',
  FINALIZED: 'bg-info',
  PAID: 'bg-success',
}

export function StatusBadge({ status }: { status: PayrollStatus }) {
  return <span className={`badge ${CLASSES[status]}`}>{status}</span>
}
