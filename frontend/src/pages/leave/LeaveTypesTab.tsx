import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { leaveTypesApi } from '../../api/leave'
import type { LeaveType } from '../../api/types'
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

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  annualDays: z.string().refine((v) => /^\d+$/.test(v.trim()), 'Enter a whole number of days'),
  paid: z.boolean(),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

function LeaveTypeForm({
  item,
  onClose,
  onSaved,
}: {
  item: LeaveType | null
  onClose: () => void
  onSaved: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: item?.name ?? '',
      annualDays: item?.annualDays.toString() ?? '0',
      paid: item?.paid ?? true,
      active: item?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = { ...(item ?? {}), name: v.name.trim(), annualDays: Number(v.annualDays), paid: v.paid, active: v.active }
    try {
      if (item) await leaveTypesApi.update(item.id, body)
      else await leaveTypesApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit leave type' : 'Add leave type'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Name" error={errors.name?.message}>
            <Input {...register('name')} />
          </Field>
          <Field label="Annual days" error={errors.annualDays?.message}>
            <Input inputMode="numeric" {...register('annualDays')} />
          </Field>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="lt-paid" {...register('paid')} />
            <label className="form-check-label" htmlFor="lt-paid">
              Paid leave
            </label>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="lt-active" {...register('active')} />
            <label className="form-check-label" htmlFor="lt-active">
              Active
            </label>
          </div>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function LeaveTypesTab() {
  const { rows, isLoading, error, remove, refresh } = useResource('leave-types', leaveTypesApi)
  const [editing, setEditing] = useState<LeaveType | 'new' | null>(null)

  const columns: Column<LeaveType>[] = [
    { header: 'Name', cell: (t) => <span className="text-dark font-weight-medium">{t.name}</span> },
    { header: 'Annual days', cell: (t) => t.annualDays },
    { header: 'Paid', cell: (t) => (t.paid ? 'Yes' : 'No') },
    { header: 'Status', cell: (t) => <ActiveBadge active={t.active} /> },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (t) => (
        <Can permission="LEAVE_MANAGE">
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(t)}>
            Edit
          </Button>
          <Button
            variant="danger"
            size="sm"
            disabled={remove.isPending}
            onClick={() => confirm(`Delete ${t.name}?`) && remove.mutate(t.id)}
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
          <Button onClick={() => setEditing('new')}>Add leave type</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
      <DataTable columns={columns} rows={rows} loading={isLoading} empty="No leave types yet." />
      {editing && (
        <LeaveTypeForm
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
