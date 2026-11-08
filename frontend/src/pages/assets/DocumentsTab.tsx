import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { documentsApi } from '../../api/assets'
import { errorMessage } from '../../api/client'
import { useEmployeeLookup } from '../../api/lookups'
import { DOCUMENT_TYPES, type EmployeeDocument } from '../../api/types'
import { useResource } from '../../api/useResource'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, Select, type Column } from '../../components/ui'
import { label, orNull } from '../../lib/format'

const schema = z.object({
  employeeId: z.string().min(1, 'Employee is required'),
  type: z.enum(DOCUMENT_TYPES),
  title: z.string().min(1, 'Title is required').max(200),
  fileUrl: z.string().min(1, 'File URL is required').max(500),
  issuedAt: z.string(),
  note: z.string().max(500),
})
type Form = z.infer<typeof schema>

function DocumentForm({
  item,
  onClose,
  onSaved,
}: {
  item: EmployeeDocument | null
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
      employeeId: item?.employeeId.toString() ?? '',
      type: item?.type ?? 'OTHER',
      title: item?.title ?? '',
      fileUrl: item?.fileUrl ?? '',
      issuedAt: item?.issuedAt ?? '',
      note: item?.note ?? '',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = {
      ...(item ?? {}),
      employeeId: Number(v.employeeId),
      type: v.type,
      title: v.title.trim(),
      fileUrl: v.fileUrl.trim(),
      issuedAt: orNull(v.issuedAt),
      note: orNull(v.note),
    }
    try {
      if (item) await documentsApi.update(item.id, body)
      else await documentsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit document' : 'Add document'} onClose={onClose}>
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
              <Field label="Type">
                <Select {...register('type')}>
                  {DOCUMENT_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {label(t)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          </div>
          <Field label="Title" error={errors.title?.message}>
            <Input {...register('title')} />
          </Field>
          <Field label="File URL" error={errors.fileUrl?.message}>
            <Input type="url" placeholder="https://..." {...register('fileUrl')} />
          </Field>
          <div className="row">
            <div className="col-md-4">
              <Field label="Issued on">
                <Input type="date" {...register('issuedAt')} />
              </Field>
            </div>
            <div className="col-md-8">
              <Field label="Note" error={errors.note?.message}>
                <Input {...register('note')} />
              </Field>
            </div>
          </div>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function DocumentsTab() {
  const employees = useEmployeeLookup()
  const [employeeId, setEmployeeId] = useState('')
  const [editing, setEditing] = useState<EmployeeDocument | 'new' | null>(null)

  const { rows, isLoading, error, remove, refresh } = useResource(
    'documents',
    documentsApi,
    employeeId ? { employeeId: Number(employeeId) } : undefined,
  )

  const columns: Column<EmployeeDocument>[] = [
    { header: 'Employee', cell: (d) => <span className="text-dark font-weight-medium">{employees.nameOf(d.employeeId)}</span> },
    { header: 'Type', cell: (d) => label(d.type) },
    {
      header: 'Title',
      cell: (d) => (
        <a href={d.fileUrl} target="_blank" rel="noreferrer noopener">
          {d.title}
        </a>
      ),
    },
    { header: 'Issued', cell: (d) => d.issuedAt ?? '-' },
    { header: 'Note', cell: (d) => d.note ?? '-' },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (d) => (
        <>
          <Can permission="DOCUMENT_UPDATE">
            <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(d)}>
              Edit
            </Button>
          </Can>
          <Can permission="DOCUMENT_DELETE">
            <Button
              variant="danger"
              size="sm"
              disabled={remove.isPending}
              onClick={() => confirm(`Delete ${d.title}?`) && remove.mutate(d.id)}
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
        {employees.employees.length > 0 ? (
          <select className="form-select w-auto" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
            <option value="">All employees</option>
            {employees.employees.map((e) => (
              <option key={e.id} value={e.id}>
                {e.fullName}
              </option>
            ))}
          </select>
        ) : (
          <span />
        )}
        <Can permission="DOCUMENT_CREATE">
          <Button onClick={() => setEditing('new')}>Add document</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
      <DataTable columns={columns} rows={rows} loading={isLoading} empty="No documents yet." />
      {editing && (
        <DocumentForm
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
