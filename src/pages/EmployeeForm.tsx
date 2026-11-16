import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { employeesApi } from '../api/employees'
import { errorMessage } from '../api/client'
import { EMPLOYMENT_TYPES, type Employee, type EmployeeRequest } from '../api/types'
import { Button, ErrorBox, Field, Input, Modal, PasswordInput, Select } from '../components/ui'

const schema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150),
  email: z.union([z.literal(''), z.string().email('Email must be valid')]),
  password: z
    .string()
    .max(100)
    .refine((v) => v === '' || v.length >= 8, 'Password must be at least 8 characters'),
  phone: z.string().max(30),
  position: z.string().max(100),
  employmentType: z.union([z.literal(''), z.enum(EMPLOYMENT_TYPES)]),
  joinedAt: z.string(),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

/** Empty form strings become null so optional backend fields stay unset. */
const orNull = (v: string) => (v.trim() === '' ? null : v.trim())

export default function EmployeeForm({
  employee,
  onClose,
  onSaved,
}: {
  employee: Employee | null
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
      fullName: employee?.fullName ?? '',
      email: employee?.email ?? '',
      password: '',
      phone: employee?.phone ?? '',
      position: employee?.position ?? '',
      employmentType: employee?.employmentType ?? '',
      joinedAt: employee?.joinedAt ?? '',
      active: employee?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    // PUT replaces the record, so carry over fields this form does not edit.
    const password = orNull(v.password)
    const body: EmployeeRequest = {
      ...(employee ?? {}),
      fullName: v.fullName.trim(),
      email: orNull(v.email),
      phone: orNull(v.phone),
      position: orNull(v.position),
      employmentType: v.employmentType === '' ? null : v.employmentType,
      joinedAt: orNull(v.joinedAt),
      active: v.active,
      ...(!employee && password ? { password } : {}),
    }
    try {
      if (employee) await employeesApi.update(employee.id, body)
      else await employeesApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={employee ? 'Edit employee' : 'Add employee'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Full name" error={errors.fullName?.message}>
            <Input {...register('fullName')} />
          </Field>
          <div className="row">
            <div className="col-md-6">
              <Field label="Email" error={errors.email?.message}>
                <Input type="email" {...register('email')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Phone" error={errors.phone?.message}>
                <Input {...register('phone')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Position" error={errors.position?.message}>
                <Input {...register('position')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Employment type">
                <Select {...register('employmentType')}>
                  <option value="">-</option>
                  {EMPLOYMENT_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t.replace('_', ' ')}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Joined on">
                <Input type="date" {...register('joinedAt')} />
              </Field>
            </div>
            {!employee && (
              <div className="col-md-6">
                <Field label="Initial password (login)" error={errors.password?.message}>
                  <PasswordInput autoComplete="new-password" {...register('password')} />
                </Field>
              </div>
            )}
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="emp-active" {...register('active')} />
            <label className="form-check-label" htmlFor="emp-active">
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
