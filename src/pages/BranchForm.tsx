import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { branchesApi } from '../api/branches'
import { errorMessage } from '../api/client'
import { useEmployeeLookup } from '../api/lookups'
import type { Branch, BranchRequest } from '../api/types'
import { Button, ErrorBox, Field, Input, Modal, Select } from '../components/ui'

const num = (label: string) =>
  z.string().refine((v) => v.trim() === '' || !Number.isNaN(Number(v)), `${label} must be a number`)

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  code: z.string().max(30),
  address: z.string().max(255),
  phone: z.string().max(30),
  managerId: z.string(),
  latitude: num('Latitude'),
  longitude: num('Longitude'),
  geofenceRadiusM: num('Radius'),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

const orNull = (v: string) => (v.trim() === '' ? null : v.trim())
const numOrNull = (v: string) => (v.trim() === '' ? null : Number(v))

export default function BranchForm({
  branch,
  onClose,
  onSaved,
}: {
  branch: Branch | null
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
      name: branch?.name ?? '',
      code: branch?.code ?? '',
      address: branch?.address ?? '',
      phone: branch?.phone ?? '',
      managerId: branch?.managerId?.toString() ?? '',
      latitude: branch?.latitude?.toString() ?? '',
      longitude: branch?.longitude?.toString() ?? '',
      geofenceRadiusM: branch?.geofenceRadiusM?.toString() ?? '',
      active: branch?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body: BranchRequest = {
      ...(branch ?? {}),
      name: v.name.trim(),
      code: orNull(v.code),
      address: orNull(v.address),
      phone: orNull(v.phone),
      managerId: numOrNull(v.managerId),
      latitude: numOrNull(v.latitude),
      longitude: numOrNull(v.longitude),
      geofenceRadiusM: numOrNull(v.geofenceRadiusM),
      active: v.active,
    }
    try {
      if (branch) await branchesApi.update(branch.id, body)
      else await branchesApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={branch ? 'Edit branch' : 'Add branch'} onClose={onClose}>
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
              <Field label="Code" error={errors.code?.message}>
                <Input {...register('code')} />
              </Field>
            </div>
            <div className="col-12">
              <Field label="Address" error={errors.address?.message}>
                <Input {...register('address')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Phone" error={errors.phone?.message}>
                <Input {...register('phone')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Manager">
                <Select {...register('managerId')}>
                  <option value="">-</option>
                  {employees.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.fullName}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Latitude" error={errors.latitude?.message}>
                <Input inputMode="decimal" {...register('latitude')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Longitude" error={errors.longitude?.message}>
                <Input inputMode="decimal" {...register('longitude')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Geofence radius (m)" error={errors.geofenceRadiusM?.message}>
                <Input inputMode="numeric" {...register('geofenceRadiusM')} />
              </Field>
            </div>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="branch-active" {...register('active')} />
            <label className="form-check-label" htmlFor="branch-active">
              Active
            </label>
          </div>
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
