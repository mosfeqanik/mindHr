import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { systemSettingsApi } from '../../api/platform'
import type { SystemSetting } from '../../api/types'
import { useResource } from '../../api/useResource'
import { Edit2, Trash2 } from 'react-feather'
import { DataTable, ErrorBox, Field, IconButton, Input, Modal, ModalFooter, PageHeader, type Column } from '../../components/ui'
import { orNull } from '../../lib/format'
import { AddButton, Card } from './shared'

const schema = z.object({
  key: z.string().min(1, 'Key is required').max(100),
  value: z.string(),
  description: z.string().max(255),
})
type Form = z.infer<typeof schema>

function SettingForm({ item, onClose, onSaved }: { item: SystemSetting | null; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: { key: item?.key ?? '', value: item?.value ?? '', description: item?.description ?? '' },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = { key: v.key.trim(), value: orNull(v.value), description: orNull(v.description) }
    try {
      if (item) await systemSettingsApi.update(item.id, body)
      else await systemSettingsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit setting' : 'Add setting'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Key" error={errors.key?.message}>
            <Input placeholder="e.g. support.email" {...register('key')} />
          </Field>
          <Field label="Value">
            <Input {...register('value')} />
          </Field>
          <Field label="Description" error={errors.description?.message}>
            <Input {...register('description')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function SystemSettingsPage() {
  const { rows, isLoading, error, remove, refresh } = useResource('system-settings', systemSettingsApi)
  const [editing, setEditing] = useState<SystemSetting | 'new' | null>(null)

  const columns: Column<SystemSetting>[] = [
    { header: 'Key', cell: (s) => <code>{s.key}</code>, value: (s) => s.key },
    { header: 'Value', cell: (s) => s.value ?? '-' },
    { header: 'Description', cell: (s) => s.description ?? '-' },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (s) => (
        <>
          <IconButton icon={<Edit2 size={15} />} label="Edit" className="me-1" onClick={() => setEditing(s)} />
          <IconButton
            icon={<Trash2 size={15} />}
            label="Delete"
            variant="danger"
            disabled={remove.isPending}
            onClick={() => confirm(`Delete setting ${s.key}?`) && remove.mutate(s.id)}
          />
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="System Settings" trail={['Platform']} />
      <Card title="Settings" actions={<AddButton onClick={() => setEditing('new')}>Add setting</AddButton>}>
        {error && <ErrorBox message={errorMessage(error)} />}
        {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
        <DataTable datatable columns={columns} rows={rows} loading={isLoading} empty="No settings yet." />
      </Card>
      {editing && (
        <SettingForm
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
