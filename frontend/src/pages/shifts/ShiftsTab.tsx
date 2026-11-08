import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { shiftsApi } from '../../api/shifts'
import type { Shift } from '../../api/types'
import { useResource } from '../../api/useResource'
import { Can } from '../../auth/Can'
import {
  ActiveBadge,
  Button,
  DataTable,
  ErrorBox,
  Field,
  Input,
  Modal,
  ModalFooter,
  type Column,
} from '../../components/ui'
import { formatTime } from '../../lib/format'

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  startTime: z.string().min(1, 'Start time is required'),
  endTime: z.string().min(1, 'End time is required'),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

function ShiftForm({ item, onClose, onSaved }: { item: Shift | null; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: item?.name ?? '',
      startTime: item ? formatTime(item.startTime) : '09:00',
      endTime: item ? formatTime(item.endTime) : '17:00',
      active: item?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = { ...(item ?? {}), name: v.name.trim(), startTime: v.startTime, endTime: v.endTime, active: v.active }
    try {
      if (item) await shiftsApi.update(item.id, body)
      else await shiftsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit shift' : 'Add shift'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Name" error={errors.name?.message}>
            <Input {...register('name')} />
          </Field>
          <div className="row">
            <div className="col-md-6">
              <Field label="Start time" error={errors.startTime?.message}>
                <Input type="time" {...register('startTime')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="End time" error={errors.endTime?.message}>
                <Input type="time" {...register('endTime')} />
              </Field>
            </div>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="shift-active" {...register('active')} />
            <label className="form-check-label" htmlFor="shift-active">
              Active
            </label>
          </div>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function ShiftsTab() {
  const { rows, isLoading, error, remove, refresh } = useResource('shifts', shiftsApi)
  const [editing, setEditing] = useState<Shift | 'new' | null>(null)

  const columns: Column<Shift>[] = [
    { header: 'Name', cell: (s) => <span className="text-dark font-weight-medium">{s.name}</span> },
    { header: 'Start', cell: (s) => formatTime(s.startTime) },
    { header: 'End', cell: (s) => formatTime(s.endTime) },
    { header: 'Status', cell: (s) => <ActiveBadge active={s.active} /> },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (s) => (
        <>
          <Can permission="SHIFT_UPDATE">
            <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(s)}>
              Edit
            </Button>
          </Can>
          <Can permission="SHIFT_DELETE">
            <Button
              variant="danger"
              size="sm"
              disabled={remove.isPending}
              onClick={() => confirm(`Delete ${s.name}?`) && remove.mutate(s.id)}
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
      <div className="d-flex justify-content-end mb-2">
        <Can permission="SHIFT_CREATE">
          <Button onClick={() => setEditing('new')}>Add shift</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
      <DataTable columns={columns} rows={rows} loading={isLoading} empty="No shifts yet." />
      {editing && (
        <ShiftForm
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
