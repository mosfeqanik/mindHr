import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { leaveRequestsApi } from '../../api/leave'
import { useEmployeeLookup, useLeaveTypeLookup } from '../../api/lookups'
import type { LeaveRequest, LeaveStatus } from '../../api/types'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, Select, type Column } from '../../components/ui'
import { label, orNull } from '../../lib/format'

const STATUS_BADGE: Record<LeaveStatus, string> = {
  PENDING: 'bg-warning text-dark',
  APPROVED: 'bg-success',
  REJECTED: 'bg-danger',
  CANCELLED: 'bg-secondary',
}

const applySchema = z
  .object({
    leaveTypeId: z.string().min(1, 'Leave type is required'),
    fromDate: z.string().min(1, 'From date is required'),
    toDate: z.string().min(1, 'To date is required'),
    reason: z.string().max(500),
  })
  .refine((v) => !v.fromDate || !v.toDate || v.toDate >= v.fromDate, {
    path: ['toDate'],
    message: 'To date must not be before from date',
  })
type ApplyForm = z.infer<typeof applySchema>

function ApplyModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { leaveTypes } = useLeaveTypeLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ApplyForm>({
    resolver: zodResolver(applySchema),
    defaultValues: { leaveTypeId: '', fromDate: '', toDate: '', reason: '' },
  })

  const onSubmit = async (v: ApplyForm) => {
    setError(null)
    try {
      await leaveRequestsApi.apply({
        leaveTypeId: Number(v.leaveTypeId),
        fromDate: v.fromDate,
        toDate: v.toDate,
        reason: orNull(v.reason),
      })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title="Apply for leave" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Leave type" error={errors.leaveTypeId?.message}>
            <Select {...register('leaveTypeId')}>
              <option value="">Select...</option>
              {leaveTypes
                .filter((t) => t.active)
                .map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
            </Select>
          </Field>
          <div className="row">
            <div className="col-md-6">
              <Field label="From" error={errors.fromDate?.message}>
                <Input type="date" {...register('fromDate')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="To" error={errors.toDate?.message}>
                <Input type="date" {...register('toDate')} />
              </Field>
            </div>
          </div>
          <Field label="Reason" error={errors.reason?.message}>
            <Input {...register('reason')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

function DecisionModal({
  request,
  kind,
  onClose,
  onDone,
}: {
  request: LeaveRequest
  kind: 'approve' | 'reject'
  onClose: () => void
  onDone: () => void
}) {
  const [note, setNote] = useState('')
  const decide = useMutation({
    mutationFn: () =>
      kind === 'approve'
        ? leaveRequestsApi.approve(request.id, orNull(note))
        : leaveRequestsApi.reject(request.id, orNull(note)),
    onSuccess: onDone,
  })

  return (
    <Modal title={kind === 'approve' ? 'Approve leave' : 'Reject leave'} onClose={onClose}>
      <div className="modal-body">
        {decide.error && <ErrorBox message={errorMessage(decide.error)} />}
        <Field label="Note (optional)">
          <Input value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button variant={kind === 'approve' ? 'primary' : 'danger'} disabled={decide.isPending} onClick={() => decide.mutate()}>
          {kind === 'approve' ? 'Approve' : 'Reject'}
        </Button>
      </div>
    </Modal>
  )
}

export default function LeaveRequestsTab() {
  const qc = useQueryClient()
  const employees = useEmployeeLookup()
  const types = useLeaveTypeLookup()
  const [status, setStatus] = useState<LeaveStatus | ''>('')
  const [applying, setApplying] = useState(false)
  const [deciding, setDeciding] = useState<{ request: LeaveRequest; kind: 'approve' | 'reject' } | null>(null)

  const { data, isLoading, error } = useQuery({ queryKey: ['leave-requests'], queryFn: () => leaveRequestsApi.list() })
  const refresh = () => qc.invalidateQueries({ queryKey: ['leave-requests'] })
  // Approving also changes the balance's used days.
  const refreshAll = () => {
    refresh()
    qc.invalidateQueries({ queryKey: ['leave-balances'] })
  }
  const cancel = useMutation({ mutationFn: (id: number) => leaveRequestsApi.cancel(id), onSuccess: refreshAll })

  const rows = (data ?? []).filter((r) => !status || r.status === status).sort((a, b) => b.fromDate.localeCompare(a.fromDate))

  const columns: Column<LeaveRequest>[] = [
    { header: 'Employee', cell: (r) => <span className="text-dark font-weight-medium">{employees.nameOf(r.employeeId)}</span> },
    { header: 'Type', cell: (r) => types.nameOf(r.leaveTypeId) },
    { header: 'From', cell: (r) => r.fromDate },
    { header: 'To', cell: (r) => r.toDate },
    { header: 'Days', cell: (r) => r.days },
    { header: 'Reason', cell: (r) => r.reason ?? '-' },
    {
      header: 'Status',
      cell: (r) => (
        <span title={r.decisionNote ?? undefined} className={`badge ${STATUS_BADGE[r.status]}`}>
          {label(r.status)}
        </span>
      ),
    },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (r) =>
        r.status === 'PENDING' ? (
          <>
            <Can permission="LEAVE_APPROVE">
              <Button size="sm" className="me-2" onClick={() => setDeciding({ request: r, kind: 'approve' })}>
                Approve
              </Button>
              <Button variant="danger" size="sm" className="me-2" onClick={() => setDeciding({ request: r, kind: 'reject' })}>
                Reject
              </Button>
            </Can>
            <Can permission="LEAVE_CREATE">
              <Button
                variant="secondary"
                size="sm"
                disabled={cancel.isPending}
                onClick={() => confirm('Cancel this leave request?') && cancel.mutate(r.id)}
              >
                Cancel
              </Button>
            </Can>
          </>
        ) : null,
    },
  ]

  return (
    <>
      <div className="d-flex justify-content-between mb-2">
        <select
          className="form-select w-auto"
          value={status}
          onChange={(e) => setStatus(e.target.value as LeaveStatus | '')}
        >
          <option value="">All statuses</option>
          {(['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'] as const).map((s) => (
            <option key={s} value={s}>
              {label(s)}
            </option>
          ))}
        </select>
        <Can permission="LEAVE_CREATE">
          <Button onClick={() => setApplying(true)}>Apply for leave</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {cancel.error && <ErrorBox message={errorMessage(cancel.error)} />}
      <DataTable columns={columns} rows={rows} loading={isLoading} empty="No leave requests." />
      {applying && (
        <ApplyModal
          onClose={() => setApplying(false)}
          onSaved={() => {
            setApplying(false)
            refresh()
          }}
        />
      )}
      {deciding && (
        <DecisionModal
          request={deciding.request}
          kind={deciding.kind}
          onClose={() => setDeciding(null)}
          onDone={() => {
            setDeciding(null)
            refreshAll()
          }}
        />
      )}
    </>
  )
}
