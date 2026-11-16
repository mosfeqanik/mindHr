import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { assetsApi } from '../../api/assets'
import { errorMessage } from '../../api/client'
import { ASSET_STATUSES, type Asset, type AssetStatus } from '../../api/types'
import { useResource } from '../../api/useResource'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, Select, type Column } from '../../components/ui'
import { label, orNull } from '../../lib/format'

export const ASSET_BADGE: Record<AssetStatus, string> = {
  AVAILABLE: 'bg-success',
  ASSIGNED: 'bg-info',
  MAINTENANCE: 'bg-warning text-dark',
  RETIRED: 'bg-secondary',
}

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  assetTag: z.string().min(1, 'Asset tag is required').max(100),
  serialNumber: z.string().max(100),
  category: z.string().max(100),
  description: z.string().max(500),
  status: z.enum(ASSET_STATUSES),
})
type Form = z.infer<typeof schema>

function AssetForm({ item, onClose, onSaved }: { item: Asset | null; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: item?.name ?? '',
      assetTag: item?.assetTag ?? '',
      serialNumber: item?.serialNumber ?? '',
      category: item?.category ?? '',
      description: item?.description ?? '',
      status: item?.status ?? 'AVAILABLE',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = {
      ...(item ?? {}),
      name: v.name.trim(),
      assetTag: v.assetTag.trim(),
      serialNumber: orNull(v.serialNumber),
      category: orNull(v.category),
      description: orNull(v.description),
      status: v.status,
    }
    try {
      if (item) await assetsApi.update(item.id, body)
      else await assetsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit asset' : 'Add asset'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-8">
              <Field label="Name" error={errors.name?.message}>
                <Input {...register('name')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Asset tag" error={errors.assetTag?.message}>
                <Input {...register('assetTag')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Category" error={errors.category?.message}>
                <Input {...register('category')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Serial number" error={errors.serialNumber?.message}>
                <Input {...register('serialNumber')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Status">
                <Select {...register('status')}>
                  {ASSET_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {label(s)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          </div>
          <Field label="Description" error={errors.description?.message}>
            <Input {...register('description')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function AssetsTab() {
  const { rows, isLoading, error, remove, refresh } = useResource('assets', assetsApi)
  const [editing, setEditing] = useState<Asset | 'new' | null>(null)
  const [status, setStatus] = useState<AssetStatus | ''>('')
  const visible = rows.filter((a) => !status || a.status === status)

  const columns: Column<Asset>[] = [
    { header: 'Tag', cell: (a) => a.assetTag },
    { header: 'Name', cell: (a) => <span className="text-dark font-weight-medium">{a.name}</span> },
    { header: 'Category', cell: (a) => a.category ?? '-' },
    { header: 'Serial', cell: (a) => a.serialNumber ?? '-' },
    { header: 'Status', cell: (a) => <span className={`badge ${ASSET_BADGE[a.status]}`}>{label(a.status)}</span> },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (a) => (
        <>
          <Can permission="ASSET_UPDATE">
            <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(a)}>
              Edit
            </Button>
          </Can>
          <Can permission="ASSET_DELETE">
            <Button
              variant="danger"
              size="sm"
              disabled={remove.isPending}
              onClick={() => confirm(`Delete ${a.name}?`) && remove.mutate(a.id)}
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
      <div className="d-flex justify-content-between mb-2">
        <select className="form-select w-auto" value={status} onChange={(e) => setStatus(e.target.value as AssetStatus | '')}>
          <option value="">All statuses</option>
          {ASSET_STATUSES.map((s) => (
            <option key={s} value={s}>
              {label(s)}
            </option>
          ))}
        </select>
        <Can permission="ASSET_CREATE">
          <Button onClick={() => setEditing('new')}>Add asset</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
      <DataTable columns={columns} rows={visible} loading={isLoading} empty="No assets yet." />
      {editing && (
        <AssetForm
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
