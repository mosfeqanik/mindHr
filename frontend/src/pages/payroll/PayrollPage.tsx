import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { useBranchLookup } from '../../api/lookups'
import { payrollApi } from '../../api/payroll'
import type { PayrollRun } from '../../api/types'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, PageHeader, Select, type Column } from '../../components/ui'
import { MONTHS, numOrNull, orNull } from '../../lib/format'
import { StatusBadge } from './StatusBadge'

const now = new Date()

const schema = z.object({
  year: z.string().refine((v) => Number(v) >= 2000, 'Year is invalid'),
  month: z.string().refine((v) => Number(v) >= 1 && Number(v) <= 12, 'Month must be 1-12'),
  branchId: z.string(),
  note: z.string().max(500),
})
type Form = z.infer<typeof schema>

function GenerateModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { branches } = useBranchLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      year: now.getFullYear().toString(),
      month: (now.getMonth() + 1).toString(),
      branchId: '',
      note: '',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    try {
      await payrollApi.generate({
        year: Number(v.year),
        month: Number(v.month),
        branchId: numOrNull(v.branchId),
        note: orNull(v.note),
      })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title="Generate payroll run" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-6">
              <Field label="Month" error={errors.month?.message}>
                <Select {...register('month')}>
                  {MONTHS.map((m, i) => (
                    <option key={m} value={i + 1}>
                      {m}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Year" error={errors.year?.message}>
                <Input inputMode="numeric" {...register('year')} />
              </Field>
            </div>
          </div>
          <Field label="Branch (leave empty for the whole organization)">
            <Select {...register('branchId')}>
              <option value="">All branches</option>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Note" error={errors.note?.message}>
            <Input {...register('note')} />
          </Field>
          <p className="text-muted small mb-0">
            Payslips are calculated from attendance, salary structures and the payroll rules, and start as a draft.
          </p>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function PayrollPage() {
  const qc = useQueryClient()
  const branches = useBranchLookup()
  const [generating, setGenerating] = useState(false)

  const { data, isLoading, error } = useQuery({ queryKey: ['payroll-runs'], queryFn: () => payrollApi.listRuns() })
  const refresh = () => qc.invalidateQueries({ queryKey: ['payroll-runs'] })
  const action = useMutation({
    mutationFn: (a: { kind: 'finalize' | 'pay' | 'delete'; id: number }) =>
      a.kind === 'finalize'
        ? payrollApi.finalize(a.id)
        : a.kind === 'pay'
          ? payrollApi.markPaid(a.id)
          : payrollApi.deleteDraft(a.id),
    onSuccess: refresh,
  })

  const rows = [...(data ?? [])].sort((a, b) => b.year * 12 + b.month - (a.year * 12 + a.month) || b.id - a.id)

  const columns: Column<PayrollRun>[] = [
    {
      header: 'Period',
      cell: (r) => (
        <Link to={`/payroll/${r.id}`} className="text-dark font-weight-medium">
          {MONTHS[r.month - 1]} {r.year}
        </Link>
      ),
    },
    { header: 'Scope', cell: (r) => (r.branchId == null ? 'Whole organization' : branches.nameOf(r.branchId)) },
    { header: 'Status', cell: (r) => <StatusBadge status={r.status} /> },
    { header: 'Note', cell: (r) => r.note ?? '-' },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (r) => (
        <>
          <Link to={`/payroll/${r.id}`} className="btn btn-outline-secondary btn-sm me-2">
            Payslips
          </Link>
          {r.status === 'DRAFT' && (
            <>
              <Can permission="PAYROLL_APPROVE">
                <Button
                  size="sm"
                  className="me-2"
                  disabled={action.isPending}
                  onClick={() => confirm('Finalize this run? Payslips can no longer be adjusted.') && action.mutate({ kind: 'finalize', id: r.id })}
                >
                  Finalize
                </Button>
              </Can>
              <Can permission="PAYROLL_DELETE">
                <Button
                  variant="danger"
                  size="sm"
                  disabled={action.isPending}
                  onClick={() => confirm('Delete this draft run?') && action.mutate({ kind: 'delete', id: r.id })}
                >
                  Delete
                </Button>
              </Can>
            </>
          )}
          {r.status === 'FINALIZED' && (
            <Can permission="PAYROLL_APPROVE">
              <Button
                size="sm"
                disabled={action.isPending}
                onClick={() => confirm('Mark this run as paid?') && action.mutate({ kind: 'pay', id: r.id })}
              >
                Mark paid
              </Button>
            </Can>
          )}
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="Payroll"
        trail={['Finance']}
        actions={
          <Can permission="PAYROLL_CREATE">
            <Button onClick={() => setGenerating(true)}>Generate run</Button>
          </Can>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <h4 className="card-title">Payroll runs</h4>
            {error && <ErrorBox message={errorMessage(error)} />}
            {action.error && <ErrorBox message={errorMessage(action.error)} />}
            <DataTable columns={columns} rows={rows} loading={isLoading} empty="No payroll runs yet." />
          </div>
        </div>
      </div>
      {generating && (
        <GenerateModal
          onClose={() => setGenerating(false)}
          onSaved={() => {
            setGenerating(false)
            refresh()
          }}
        />
      )}
    </>
  )
}
