import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { attendanceApi } from '../api/attendance'
import { errorMessage } from '../api/client'
import { useEmployeeLookup } from '../api/lookups'
import {
  ATTENDANCE_METHODS,
  ATTENDANCE_STATUSES,
  type Attendance,
  type AttendanceRequest,
} from '../api/types'
import { Button, ErrorBox, Field, Input, Modal, Select } from '../components/ui'

const int = (label: string) =>
  z.string().refine((v) => v.trim() === '' || /^\d+$/.test(v.trim()), `${label} must be a whole number`)

const schema = z.object({
  employeeId: z.string().min(1, 'Employee is required'),
  workDate: z.string().min(1, 'Work date is required'),
  checkInAt: z.string(),
  checkOutAt: z.string(),
  method: z.union([z.literal(''), z.enum(ATTENDANCE_METHODS)]),
  status: z.union([z.literal(''), z.enum(ATTENDANCE_STATUSES)]),
  lateMinutes: int('Late minutes'),
  workedMinutes: int('Worked minutes'),
  note: z.string().max(500),
})
type Form = z.infer<typeof schema>

// <input type="datetime-local"> wants "YYYY-MM-DDTHH:mm"; the API returns seconds too.
const toLocal = (v: string | null | undefined) => (v ? v.slice(0, 16) : '')
const dateTimeOrNull = (v: string) => (v === '' ? null : v.length === 16 ? `${v}:00` : v)
const intOrUndefined = (v: string) => (v.trim() === '' ? undefined : Number(v))

export default function AttendanceForm({
  record,
  onClose,
  onSaved,
}: {
  record: Attendance | null
  onClose: () => void
  onSaved: () => void
}) {
  const { employees } = useEmployeeLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      employeeId: record?.employeeId.toString() ?? '',
      workDate: record?.workDate ?? new Date().toISOString().slice(0, 10),
      checkInAt: toLocal(record?.checkInAt),
      checkOutAt: toLocal(record?.checkOutAt),
      method: record?.method ?? 'MANUAL',
      status: record?.status ?? '',
      lateMinutes: record?.lateMinutes?.toString() ?? '',
      workedMinutes: record?.workedMinutes?.toString() ?? '',
      note: record?.note ?? '',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body: AttendanceRequest = {
      ...(record ?? {}),
      employeeId: Number(v.employeeId),
      workDate: v.workDate,
      checkInAt: dateTimeOrNull(v.checkInAt),
      checkOutAt: dateTimeOrNull(v.checkOutAt),
      method: v.method === '' ? null : v.method,
      status: v.status === '' ? null : v.status,
      lateMinutes: intOrUndefined(v.lateMinutes),
      workedMinutes: intOrUndefined(v.workedMinutes),
      note: v.note.trim() === '' ? null : v.note.trim(),
    }
    try {
      if (record) await attendanceApi.update(record.id, body)
      else await attendanceApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={record ? 'Edit attendance' : 'Add attendance'} onClose={onClose}>
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
                      {e.fullName} ({e.employeeCode})
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Work date" error={errors.workDate?.message}>
                <Input type="date" {...register('workDate')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Check in">
                <Input type="datetime-local" {...register('checkInAt')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Check out">
                <Input type="datetime-local" {...register('checkOutAt')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Status">
                <Select {...register('status')}>
                  <option value="">-</option>
                  {ATTENDANCE_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s.replace('_', ' ')}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Method">
                <Select {...register('method')}>
                  <option value="">-</option>
                  {ATTENDANCE_METHODS.map((m) => (
                    <option key={m} value={m}>
                      {m.replace('_', ' ')}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Late (minutes)" error={errors.lateMinutes?.message}>
                <Input inputMode="numeric" {...register('lateMinutes')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Worked (minutes)" error={errors.workedMinutes?.message}>
                <Input inputMode="numeric" {...register('workedMinutes')} />
              </Field>
            </div>
          </div>
          <Field label="Note" error={errors.note?.message}>
            <Input {...register('note')} />
          </Field>
        </div>
        <div className="modal-footer">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Save'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
