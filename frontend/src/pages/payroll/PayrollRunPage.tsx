import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { useEmployeeLookup } from '../../api/lookups'
import { payrollApi } from '../../api/payroll'
import type { Payslip } from '../../api/types'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, PageHeader, type Column } from '../../components/ui'
import { MONTHS, formatMoney } from '../../lib/format'
import { StatusBadge } from './StatusBadge'

const amount = (label: string) =>
  z.string().refine((v) => v.trim() !== '' && Number(v) >= 0, `${label} must be 0 or more`)

const schema = z.object({
  overtime: amount('Overtime'),
  bonus: amount('Bonus'),
  commission: amount('Commission'),
  loanDeduction: amount('Loan deduction'),
  advanceDeduction: amount('Advance deduction'),
})
type Form = z.infer<typeof schema>

const FIELDS: { name: keyof Form; label: string }[] = [
  { name: 'overtime', label: 'Overtime' },
  { name: 'bonus', label: 'Bonus' },
  { name: 'commission', label: 'Commission' },
  { name: 'loanDeduction', label: 'Loan deduction' },
  { name: 'advanceDeduction', label: 'Advance deduction' },
]

function AdjustModal({ slip, name, onClose, onSaved }: { slip: Payslip; name: string; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      overtime: slip.overtime.toString(),
      bonus: slip.bonus.toString(),
      commission: slip.commission.toString(),
      loanDeduction: slip.loanDeduction.toString(),
      advanceDeduction: slip.advanceDeduction.toString(),
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    try {
      await payrollApi.adjust(slip.id, {
        overtime: Number(v.overtime),
        bonus: Number(v.bonus),
        commission: Number(v.commission),
        loanDeduction: Number(v.loanDeduction),
        advanceDeduction: Number(v.advanceDeduction),
      })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={`Adjust payslip - ${name}`} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            {FIELDS.map((f) => (
              <div key={f.name} className="col-md-6">
                <Field label={f.label} error={errors[f.name]?.message}>
                  <Input inputMode="decimal" {...register(f.name)} />
                </Field>
              </div>
            ))}
          </div>
          <p className="text-muted small mb-0">Gross and net salary are recalculated by the server.</p>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function PayrollRunPage() {
  const id = Number(useParams().id)
  const qc = useQueryClient()
  const { nameOf } = useEmployeeLookup()
  const [adjusting, setAdjusting] = useState<Payslip | null>(null)

  const run = useQuery({ queryKey: ['payroll-runs', id], queryFn: () => payrollApi.getRun(id) })
  // The payslip list has no run filter, so filter here.
  const slips = useQuery({ queryKey: ['payslips'], queryFn: () => payrollApi.listPayslips() })
  const rows = (slips.data ?? []).filter((p) => p.payrollRunId === id)

  const totalNet = rows.reduce((sum, p) => sum + p.netSalary, 0)
  const isDraft = run.data?.status === 'DRAFT'

  const columns: Column<Payslip>[] = [
    { header: 'Employee', cell: (p) => <span className="text-dark font-weight-medium">{nameOf(p.employeeId)}</span> },
    { header: 'Basic', className: 'text-end', cell: (p) => formatMoney(p.basicSalary) },
    { header: 'Allowances', className: 'text-end', cell: (p) => formatMoney(p.allowances) },
    { header: 'Overtime', className: 'text-end', cell: (p) => formatMoney(p.overtime) },
    { header: 'Bonus', className: 'text-end', cell: (p) => formatMoney(p.bonus + p.commission) },
    { header: 'Gross', className: 'text-end', cell: (p) => formatMoney(p.grossSalary) },
    {
      header: 'Deductions',
      className: 'text-end',
      cell: (p) => formatMoney(p.lateDeduction + p.absentDeduction + p.loanDeduction + p.advanceDeduction),
    },
    { header: 'Net', className: 'text-end', cell: (p) => <strong>{formatMoney(p.netSalary)}</strong> },
    { header: 'P / A / L', cell: (p) => `${p.daysPresent} / ${p.daysAbsent} / ${p.daysLate}` },
    {
      header: '',
      className: 'text-end',
      cell: (p) =>
        isDraft ? (
          <Can permission="PAYROLL_UPDATE">
            <Button variant="secondary" size="sm" onClick={() => setAdjusting(p)}>
              Adjust
            </Button>
          </Can>
        ) : null,
    },
  ]

  const title = run.data ? `${MONTHS[run.data.month - 1]} ${run.data.year}` : 'Payroll run'

  return (
    <>
      <PageHeader
        title={title}
        trail={['Payroll']}
        actions={
          <Link to="/payroll" className="btn btn-outline-secondary">
            Back to runs
          </Link>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            {run.error && <ErrorBox message={errorMessage(run.error)} />}
            {slips.error && <ErrorBox message={errorMessage(slips.error)} />}
            <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
              {run.data && <StatusBadge status={run.data.status} />}
              <span className="text-muted">{rows.length} payslips</span>
              <span>
                Total net: <strong>{formatMoney(totalNet)}</strong>
              </span>
              {run.data?.note && <span className="text-muted">{run.data.note}</span>}
            </div>
            <DataTable columns={columns} rows={rows} loading={slips.isLoading} empty="No payslips in this run." />
          </div>
        </div>
      </div>
      {adjusting && (
        <AdjustModal
          slip={adjusting}
          name={nameOf(adjusting.employeeId)}
          onClose={() => setAdjusting(null)}
          onSaved={() => {
            setAdjusting(null)
            qc.invalidateQueries({ queryKey: ['payslips'] })
          }}
        />
      )}
    </>
  )
}
