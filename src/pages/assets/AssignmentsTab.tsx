import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { assetAssignmentsApi } from '../../api/assets'
import { errorMessage } from '../../api/client'
import { useAssetLookup, useEmployeeLookup } from '../../api/lookups'
import type { AssetAssignment } from '../../api/types'
import { Can } from '../../auth/Can'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, Select, type Column } from '../../components/ui'
import { label, orNull } from '../../lib/format'

const today = () => new Date().toISOString().slice(0, 10)

const assignSchema = z.object({
  assetId: z.string().min(1, 'Asset is required'),
  employeeId: z.string().min(1, 'Employee is required'),
  assignedAt: z.string().min(1, 'Date is required'),
})
type AssignForm = z.infer<typeof assignSchema>

function AssignModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { assets } = useAssetLookup()
  const { employees } = useEmployeeLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AssignForm>({
    resolver: zodResolver(assignSchema),
    defaultValues: { assetId: '', employeeId: '', assignedAt: today() },
  })

  const onSubmit = async (v: AssignForm) => {
    setError(null)
    try {
      await assetAssignmentsApi.assign({
        assetId: Number(v.assetId),
        employeeId: Number(v.employeeId),
        assignedAt: v.assignedAt,
      })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title="Assign asset" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Asset (available only)" error={errors.assetId?.message}>
            <Select {...register('assetId')}>
              <option value="">Select...</option>
              {assets
                .filter((a) => a.status === 'AVAILABLE')
                .map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} ({a.assetTag})
                  </option>
                ))}
            </Select>
          </Field>
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
          <Field label="Assigned on" error={errors.assignedAt?.message}>
            <Input type="date" {...register('assignedAt')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

const RETURN_STATUSES = ['AVAILABLE', 'MAINTENANCE', 'RETIRED'] as const

const returnSchema = z.object({
  returnedAt: z.string().min(1, 'Date is required'),
  conditionNote: z.string().max(500),
  assetStatus: z.enum(RETURN_STATUSES),
})
type ReturnForm = z.infer<typeof returnSchema>

function ReturnModal({
  item,
  onClose,
  onSaved,
}: {
  item: AssetAssignment
  onClose: () => void
  onSaved: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ReturnForm>({
    resolver: zodResolver(returnSchema),
    defaultValues: { returnedAt: today(), conditionNote: '', assetStatus: 'AVAILABLE' },
  })

  const onSubmit = async (v: ReturnForm) => {
    setError(null)
    try {
      await assetAssignmentsApi.returnAsset(item.id, {
        returnedAt: v.returnedAt,
        conditionNote: orNull(v.conditionNote),
        assetStatus: v.assetStatus,
      })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title="Return asset" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-6">
              <Field label="Returned on" error={errors.returnedAt?.message}>
                <Input type="date" {...register('returnedAt')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Asset status after return">
                <Select {...register('assetStatus')}>
                  {RETURN_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {label(s)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          </div>
          <Field label="Condition note" error={errors.conditionNote?.message}>
            <Input {...register('conditionNote')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

export default function AssignmentsTab() {
  const qc = useQueryClient()
  const assets = useAssetLookup()
  const employees = useEmployeeLookup()
  const [employeeId, setEmployeeId] = useState('')
  const [onlyOpen, setOnlyOpen] = useState(false)
  const [assigning, setAssigning] = useState(false)
  const [returning, setReturning] = useState<AssetAssignment | null>(null)

  const params = employeeId ? { employeeId: Number(employeeId) } : undefined
  const { data, isLoading, error } = useQuery({
    queryKey: ['asset-assignments', params ?? null],
    queryFn: () => assetAssignmentsApi.list(params),
  })
  // Assign and return change the asset status as well.
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['asset-assignments'] })
    qc.invalidateQueries({ queryKey: ['assets'] })
  }

  const rows = (data ?? [])
    .filter((a) => !onlyOpen || !a.returnedAt)
    .sort((a, b) => b.assignedAt.localeCompare(a.assignedAt) || b.id - a.id)

  const columns: Column<AssetAssignment>[] = [
    { header: 'Asset', cell: (a) => <span className="text-dark font-weight-medium">{assets.nameOf(a.assetId)}</span> },
    { header: 'Employee', cell: (a) => employees.nameOf(a.employeeId) },
    { header: 'Assigned', cell: (a) => a.assignedAt },
    {
      header: 'Returned',
      cell: (a) => (a.returnedAt ? a.returnedAt : <span className="badge bg-info">With employee</span>),
    },
    { header: 'Condition', cell: (a) => a.conditionNote ?? '-' },
    {
      header: '',
      className: 'text-end',
      cell: (a) =>
        a.returnedAt ? null : (
          <Can permission="ASSET_UPDATE">
            <Button variant="secondary" size="sm" onClick={() => setReturning(a)}>
              Return
            </Button>
          </Can>
        ),
    },
  ]

  return (
    <>
      <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
        <div className="d-flex align-items-center gap-3">
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
          <div className="form-check mb-0">
            <input
              className="form-check-input"
              type="checkbox"
              id="only-open"
              checked={onlyOpen}
              onChange={(e) => setOnlyOpen(e.target.checked)}
            />
            <label className="form-check-label" htmlFor="only-open">
              Not returned
            </label>
          </div>
        </div>
        <Can permission="ASSET_CREATE">
          <Button onClick={() => setAssigning(true)}>Assign asset</Button>
        </Can>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      <DataTable columns={columns} rows={rows} loading={isLoading} empty="No asset assignments." />
      {assigning && (
        <AssignModal
          onClose={() => setAssigning(false)}
          onSaved={() => {
            setAssigning(false)
            refresh()
          }}
        />
      )}
      {returning && (
        <ReturnModal
          item={returning}
          onClose={() => setReturning(null)}
          onSaved={() => {
            setReturning(null)
            refresh()
          }}
        />
      )}
    </>
  )
}
