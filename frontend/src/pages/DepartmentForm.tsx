import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { departmentsApi } from '../api/departments'
import { errorMessage } from '../api/client'
import { useBranchLookup, useEmployeeLookup } from '../api/lookups'
import type { Department, DepartmentRequest } from '../api/types'
import { Button, ErrorBox, Field, Input, Modal, Select } from '../components/ui'

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  branchId: z.string(),
  headId: z.string(),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

const numOrNull = (v: string) => (v === '' ? null : Number(v))

export default function DepartmentForm({
  department,
  onClose,
  onSaved,
}: {
  department: Department | null
  onClose: () => void
  onSaved: () => void
}) {
  const { branches } = useBranchLookup()
  const { employees } = useEmployeeLookup()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: department?.name ?? '',
      branchId: department?.branchId?.toString() ?? '',
      headId: department?.headId?.toString() ?? '',
      active: department?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body: DepartmentRequest = {
      ...(department ?? {}),
      name: v.name.trim(),
      branchId: numOrNull(v.branchId),
      headId: numOrNull(v.headId),
      active: v.active,
    }
    try {
      if (department) await departmentsApi.update(department.id, body)
      else await departmentsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={department ? 'Edit department' : 'Add department'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Name" error={errors.name?.message}>
            <Input {...register('name')} />
          </Field>
          <div className="row">
            <div className="col-md-6">
              <Field label="Branch">
                <Select {...register('branchId')}>
                  <option value="">-</option>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Head">
                <Select {...register('headId')}>
                  <option value="">-</option>
                  {employees.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.fullName}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="dept-active" {...register('active')} />
            <label className="form-check-label" htmlFor="dept-active">
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
