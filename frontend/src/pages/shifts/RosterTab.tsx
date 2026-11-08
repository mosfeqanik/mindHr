import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { useEmployeeLookup, useShiftLookup } from '../../api/lookups'
import { shiftAssignmentsApi } from '../../api/shifts'
import type { ShiftAssignment } from '../../api/types'
import { useResource } from '../../api/useResource'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, Select, type Column } from '../../components/ui'
import { formatTime } from '../../lib/format'

const schema = z.object({
  employeeId: z.string().min(1, 'Employee is required'),
  shiftId: z.string().min(1, 'Shift is required'),
  workDate: z.string().min(1, 'Work date is required'),
})
type Form = z.infer<typeof schema>

function AssignmentForm({
  item,
  onClose,
  onSaved,
}: {
  item: ShiftAssignment | null
  onClose: () => void
  onSaved: () => void
}) {
  const { employees } = useEmployeeLookup()
  const { shifts } = useShiftLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      employeeId: item?.employeeId.toString() ?? '',
      shiftId: item?.shiftId.toString() ?? '',
      workDate: item?.workDate ?? new Date().toISOString().slice(0, 10),
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = { ...(item ?? {}), employeeId: Number(v.employeeId), shiftId: Number(v.shiftId), workDate: v.workDate }
    try {
      if (item) await shiftAssignmentsApi.update(item.id, body)
      else await shiftAssignmentsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit assignment' : 'Assign shift'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Employee" error={errors.employeeId?.message}>
            <Select {...register('employeeId')}>
              <option value="">Select...</option>
              {employees.map((e) => (
                <option key={e.id} value={e.id}>
                  {e.fullName} ({e.employeeCode})
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Shift" error={errors.shiftId?.message}>
            <Select {...register('shiftId')}>
              <option value="">Select...</option>
              {shifts
                .filter((s) => s.active || s.id === item?.shiftId)
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({formatTime(s.startTime)}-{formatTime(s.endTime)})
                  </option>
                ))}
            </Select>
          </Field>
          <Field label="Work date" error={errors.workDate?.message}>
            <Input type="date" {...register('workDate')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function RosterTab() {
  const employees = useEmployeeLookup()
  const shifts = useShiftLookup()
  const [employeeId, setEmployeeId] = useState('')
  const [date, setDate] = useState('')
  const [editing, setEditing] = useState<ShiftAssignment | 'new' | null>(null)

  const { rows, isLoading, error, remove, refresh } = useResource(
    'shift-assignments',
    shiftAssignmentsApi,
    employeeId ? { employeeId: Number(employeeId) } : undefined,
  )
  const visible = rows.filter((a) => !date || a.workDate === date).sort((a, b) => b.workDate.localeCompare(a.workDate))

  const columns: Column<ShiftAssignment>[] = [
    { header: 'Date', cell: (a) => a.workDate },
    { header: 'Employee', cell: (a) => <span className="text-dark font-weight-medium">{employees.nameOf(a.employeeId)}</span> },
    { header: 'Shift', cell: (a) => shifts.nameOf(a.shiftId) },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (a) => (
        <>
          <Can permission="SHIFT_UPDATE">
            <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(a)}>
              Edit
            </Button>
          </Can>
          <Can permission="SHIFT_DELETE">
            <Button
              variant="danger"
              size="sm"
              disabled={remove.isPending}
              onClick={() => confirm('Remove this assignment?') && remove.mutate(a.id)}
            >
              Delete
            </Button>
          </Can>
        </>
      ),
    },
  ]

  return (
    <>
      <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
        <div className="d-flex gap-2">
          {employees.employees.length > 0 && (
            <select className="form-select w-auto" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              <option value="">All employees</option>
              {employees.employees.map((e) => (
                <option key={e.id} value={e.id}>
                  {e.fullName}
                </option>
              ))}
            </select>
          )}
          <input type="date" className="form-control w-auto" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <Can permission="SHIFT_CREATE">
          <Button onClick={() => setEditing('new')}>Assign shift</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
      <DataTable columns={columns} rows={visible} loading={isLoading} empty="No shifts assigned." />
      {editing && (
        <AssignmentForm
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
