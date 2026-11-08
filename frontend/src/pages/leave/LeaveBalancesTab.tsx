import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { leaveBalancesApi } from '../../api/leave'
import { useEmployeeLookup, useLeaveTypeLookup } from '../../api/lookups'
import type { LeaveBalance } from '../../api/types'
import { useResource } from '../../api/useResource'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, Select, type Column } from '../../components/ui'

const days = (label: string) =>
  z.string().refine((v) => v.trim() !== '' && Number(v) >= 0, `${label} must be 0 or more`)

const schema = z.object({
  employeeId: z.string().min(1, 'Employee is required'),
  leaveTypeId: z.string().min(1, 'Leave type is required'),
  year: z.string().refine((v) => /^\d{4}$/.test(v.trim()), 'Enter a 4-digit year'),
  entitledDays: days('Entitled days'),
  usedDays: days('Used days'),
})
type Form = z.infer<typeof schema>

function BalanceForm({
  item,
  onClose,
  onSaved,
}: {
  item: LeaveBalance | null
  onClose: () => void
  onSaved: () => void
}) {
  const { employees } = useEmployeeLookup()
  const { leaveTypes } = useLeaveTypeLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      employeeId: item?.employeeId.toString() ?? '',
      leaveTypeId: item?.leaveTypeId.toString() ?? '',
      year: (item?.year ?? new Date().getFullYear()).toString(),
      entitledDays: item?.entitledDays.toString() ?? '0',
      usedDays: item?.usedDays.toString() ?? '0',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = {
      ...(item ?? {}),
      employeeId: Number(v.employeeId),
      leaveTypeId: Number(v.leaveTypeId),
      year: Number(v.year),
      entitledDays: Number(v.entitledDays),
      usedDays: Number(v.usedDays),
    }
    try {
      if (item) await leaveBalancesApi.update(item.id, body)
      else await leaveBalancesApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit leave balance' : 'Add leave balance'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-6">
              <Field label="Employee" error={errors.employeeId?.message}>
                <Select {...register('employeeId')}>
                  <option value="">Select...</option>
                  {employees.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.fullName}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Leave type" error={errors.leaveTypeId?.message}>
                <Select {...register('leaveTypeId')}>
                  <option value="">Select...</option>
                  {leaveTypes.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Year" error={errors.year?.message}>
                <Input inputMode="numeric" {...register('year')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Entitled days" error={errors.entitledDays?.message}>
                <Input inputMode="decimal" {...register('entitledDays')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Used days" error={errors.usedDays?.message}>
                <Input inputMode="decimal" {...register('usedDays')} />
              </Field>
            </div>
          </div>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function LeaveBalancesTab() {
  const { rows, isLoading, error, remove, refresh } = useResource('leave-balances', leaveBalancesApi)
  const employees = useEmployeeLookup()
  const types = useLeaveTypeLookup()
  const [editing, setEditing] = useState<LeaveBalance | 'new' | null>(null)

  const columns: Column<LeaveBalance>[] = [
    { header: 'Employee', cell: (b) => <span className="text-dark font-weight-medium">{employees.nameOf(b.employeeId)}</span> },
    { header: 'Leave type', cell: (b) => types.nameOf(b.leaveTypeId) },
    { header: 'Year', cell: (b) => b.year },
    { header: 'Entitled', cell: (b) => b.entitledDays },
    { header: 'Used', cell: (b) => b.usedDays },
    { header: 'Remaining', cell: (b) => <strong>{b.entitledDays - b.usedDays}</strong> },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (b) => (
        <Can permission="LEAVE_MANAGE">
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(b)}>
            Edit
          </Button>
          <Button
            variant="danger"
            size="sm"
            disabled={remove.isPending}
            onClick={() => confirm('Delete this balance?') && remove.mutate(b.id)}
          >
            Delete
          </Button>
        </Can>
      ),
    },
  ]

  return (
    <>
      <div className="d-flex justify-content-end mb-2">
        <Can permission="LEAVE_MANAGE">
          <Button onClick={() => setEditing('new')}>Add balance</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
      <DataTable columns={columns} rows={rows} loading={isLoading} empty="No leave balances yet." />
      {editing && (
        <BalanceForm
          item={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            refresh()
          }}
        />
      )}
    </>
  )
}
