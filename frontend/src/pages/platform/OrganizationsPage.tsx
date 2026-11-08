import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Edit2, Eye, Trash2, UserCheck } from 'react-feather'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { organizationsApi, subscriptionPlansApi, usersApi } from '../../api/platform'
import { ORGANIZATION_STATUSES, type Organization, type OrganizationStatus } from '../../api/types'
import { useResource } from '../../api/useResource'
import {
  Button,
  DataTable,
  IconButton,
  Textarea,
  ErrorBox,
  Field,
  Input,
  Modal,
  ModalFooter,
  PageHeader,
  PasswordInput,
  Select,
  type Column,
} from '../../components/ui'
import { formatMoney, label, orNull } from '../../lib/format'
import { AddButton, Card } from './shared'

const STATUS_BADGE: Record<OrganizationStatus, string> = {
  TRIAL: 'bg-info',
  ACTIVE: 'bg-success',
  EXPIRED: 'bg-secondary',
  SUSPENDED: 'bg-danger',
}

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  status: z.enum(ORGANIZATION_STATUSES),
  email: z.union([z.literal(''), z.string().email('Email must be valid')]),
  phone: z.string().max(30),
  address: z.string().max(500),
  trialEndsAt: z.string(),
  planId: z.string(),
})
type Form = z.infer<typeof schema>
// The plan is picked only when creating: the organization starts with a subscription to it.
const createSchema = schema.refine((v) => v.planId !== '', { message: 'Subscription plan is required', path: ['planId'] })

const toLocal = (v: string | null | undefined) => (v ? v.slice(0, 16) : '')

/** Local "yyyy-MM-ddTHH:mm" for now + days, the value format of a datetime-local input. */
function inDays(days: number) {
  const d = new Date(Date.now() + days * 86_400_000)
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

function OrganizationForm({
  item,
  onClose,
  onSaved,
}: {
  item: Organization | null
  onClose: () => void
  onSaved: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const plans = useQuery({
    queryKey: ['subscription-plans'],
    queryFn: () => subscriptionPlansApi.list(),
    enabled: !item,
  })
  const activePlans = useMemo(() => (plans.data ?? []).filter((p) => p.active), [plans.data])
  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(item ? schema : createSchema),
    defaultValues: {
      name: item?.name ?? '',
      status: item?.status ?? 'TRIAL',
      email: item?.email ?? '',
      phone: item?.phone ?? '',
      address: item?.address ?? '',
      trialEndsAt: toLocal(item?.trialEndsAt),
      planId: '',
    },
  })

  // Preselect the Basic plan (or the first active one) once the plans load.
  useEffect(() => {
    if (item || activePlans.length === 0 || getValues('planId')) return
    const basic = activePlans.find((p) => p.name.trim().toLowerCase() === 'basic') ?? activePlans[0]
    setValue('planId', String(basic.id))
  }, [item, activePlans, getValues, setValue])

  // Fill "Trial ends" from the plan's trial days, unless the user typed their own date.
  const planId = watch('planId')
  const autoTrial = useRef('')
  useEffect(() => {
    const plan = activePlans.find((p) => String(p.id) === planId)
    if (item || !plan) return
    const current = getValues('trialEndsAt')
    if (current !== '' && current !== autoTrial.current) return
    autoTrial.current = plan.trialDays > 0 ? inDays(plan.trialDays) : ''
    setValue('trialEndsAt', autoTrial.current)
  }, [item, planId, activePlans, getValues, setValue])

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = {
      name: v.name.trim(),
      status: v.status,
      email: orNull(v.email),
      phone: orNull(v.phone),
      address: orNull(v.address),
      trialEndsAt: v.trialEndsAt ? `${v.trialEndsAt}:00` : null,
      planId: item ? undefined : Number(v.planId),
    }
    try {
      if (item) await organizationsApi.update(item.id, body)
      else await organizationsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit organization' : 'Add organization'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          {plans.error && <ErrorBox message={errorMessage(plans.error)} />}
          <div className="row">
            <div className="col-12">
              <Field label="Name" error={errors.name?.message}>
                <Input {...register('name')} />
              </Field>
            </div>
            {!item && (
              <div className="col-12">
                <Field label="Subscription plan" error={errors.planId?.message}>
                  <Select {...register('planId')} disabled={plans.isLoading}>
                    <option value="">{plans.isLoading ? 'Loading plans...' : 'Select...'}</option>
                    {activePlans.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} - {formatMoney(p.price)} {p.currency ?? ''} / {label(p.billingCycle).toLowerCase()}
                        {p.trialDays > 0 ? ` (${p.trialDays}-day trial)` : ''}
                      </option>
                    ))}
                  </Select>
                </Field>
              </div>
            )}
            <div className="col-md-6">
              <Field label="Status">
                <Select {...register('status')}>
                  {ORGANIZATION_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {label(s)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Trial ends">
                <Input type="datetime-local" {...register('trialEndsAt')} />
              </Field>
            </div>
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
          </div>
          <Field label="Address" error={errors.address?.message}>
            <Textarea {...register('address')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

const ownerSchema = z.object({
  fullName: z.string().trim().min(1, 'Full name is required').max(150),
  email: z.string().trim().email('Email must be valid').max(255),
  phone: z.string().max(30),
  password: z.string().min(8, 'Password must be at least 8 characters').max(100),
})
type OwnerForm = z.infer<typeof ownerSchema>

/** Picks an existing user of the organization as owner, or creates the owner user and sets it in one step. */
function OwnerModal({ org, onClose, onSaved }: { org: Organization; onClose: () => void; onSaved: () => void }) {
  const [userId, setUserId] = useState(org.ownerUserId?.toString() ?? '')
  const [mode, setMode] = useState<'existing' | 'new' | null>(null)
  const users = useQuery({
    queryKey: ['users', { organizationId: org.id }],
    queryFn: () => usersApi.list({ organizationId: org.id }),
  })
  // Default to "new user" when the organization has nobody to pick yet.
  const activeMode = mode ?? (users.data && users.data.length === 0 ? 'new' : 'existing')

  const setOwner = useMutation({
    mutationFn: (id: number) => organizationsApi.setOwner(org.id, id),
    onSuccess: onSaved,
  })
  const createOwner = useMutation({
    mutationFn: async (v: OwnerForm) => {
      const user = await usersApi.create({
        organizationId: org.id,
        fullName: v.fullName.trim(),
        email: v.email.trim(),
        phone: orNull(v.phone),
        password: v.password,
        userType: 'ORG_USER',
        active: true,
      })
      return organizationsApi.setOwner(org.id, user.id)
    },
    onSuccess: onSaved,
  })
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OwnerForm>({
    resolver: zodResolver(ownerSchema),
    defaultValues: { fullName: '', email: org.email ?? '', phone: org.phone ?? '', password: '' },
  })

  const saving = setOwner.isPending || createOwner.isPending
  const error = setOwner.error ?? createOwner.error ?? users.error

  return (
    <Modal title={`Owner of ${org.name}`} onClose={onClose}>
      <form
        noValidate
        onSubmit={
          activeMode === 'new'
            ? handleSubmit((v) => createOwner.mutate(v))
            : (e) => {
                e.preventDefault()
                if (userId) setOwner.mutate(Number(userId))
              }
        }
      >
        <div className="modal-body">
          {error && <ErrorBox message={errorMessage(error)} />}
          <div className="btn-group mb-3" role="group">
            <Button
              type="button"
              variant={activeMode === 'existing' ? 'primary' : 'secondary'}
              onClick={() => setMode('existing')}
            >
              Existing user
            </Button>
            <Button
              type="button"
              variant={activeMode === 'new' ? 'primary' : 'secondary'}
              onClick={() => setMode('new')}
            >
              New user
            </Button>
          </div>

          {activeMode === 'existing' ? (
            <>
              <Field label="Owner user">
                <Select value={userId} onChange={(e) => setUserId(e.target.value)} disabled={users.isLoading}>
                  <option value="">{users.isLoading ? 'Loading users...' : 'Select...'}</option>
                  {(users.data ?? []).map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.fullName} ({u.email})
                    </option>
                  ))}
                </Select>
              </Field>
              {users.data?.length === 0 && (
                <p className="text-muted small mb-0">This organization has no users yet. Use "New user" to create the owner.</p>
              )}
            </>
          ) : (
            <div className="row">
              <div className="col-md-6">
                <Field label="Full name" error={errors.fullName?.message}>
                  <Input {...register('fullName')} />
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Email (login)" error={errors.email?.message}>
                  <Input type="email" {...register('email')} />
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Phone" error={errors.phone?.message}>
                  <Input {...register('phone')} />
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Password" error={errors.password?.message}>
                  <PasswordInput autoComplete="new-password" {...register('password')} />
                </Field>
              </div>            </div>
          )}
        </div>
        <div className="modal-footer">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving || (activeMode === 'existing' && !userId)}>
            {saving ? 'Saving...' : activeMode === 'new' ? 'Create owner' : 'Set owner'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

const formatDateTime = (v: string | null | undefined) => (v ? v.slice(0, 16).replace('T', ' ') : '-')

/** Read-only details of an organization, with a shortcut to the edit form. */
function OrganizationDetails({ org, onClose, onEdit }: { org: Organization; onClose: () => void; onEdit: () => void }) {
  const rows: [string, React.ReactNode][] = [
    ['Name', org.name],
    ['Slug', org.slug],
    ['Status', <span className={`badge ${STATUS_BADGE[org.status]}`}>{label(org.status)}</span>],
    ['Email', org.email ?? '-'],
    ['Phone', org.phone ?? '-'],
    ['Address', org.address ?? '-'],
    ['Owner', org.ownerUserId ? `${org.ownerName} (${org.ownerEmail})` : 'Not set'],
    ['Trial ends', formatDateTime(org.trialEndsAt)],
    ['Activated', formatDateTime(org.activatedAt)],
  ]
  return (
    <Modal title={org.name} onClose={onClose}>
      <div className="modal-body">
        <table className="table table-sm mb-0">
          <tbody>
            {rows.map(([k, v]) => (
              <tr key={k}>
                <th className="text-muted fw-normal w-25">{k}</th>
                <td>{v}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
        <Button onClick={onEdit}>
          <Edit2 size={15} className="me-1" />
          Edit
        </Button>
      </div>
    </Modal>
  )
}

export default function OrganizationsPage() {
  const { rows, isLoading, error, remove, refresh } = useResource('organizations', organizationsApi)
  const [editing, setEditing] = useState<Organization | 'new' | null>(null)
  const [ownerOf, setOwnerOf] = useState<Organization | null>(null)
  const [viewing, setViewing] = useState<Organization | null>(null)

  const columns: Column<Organization>[] = [
    { header: 'ID', cell: (o) => o.id },
    { header: 'Name', cell: (o) => <span className="text-dark font-weight-medium">{o.name}</span>, value: (o) => o.name },
    {
      header: 'Status',
      cell: (o) => <span className={`badge ${STATUS_BADGE[o.status]}`}>{label(o.status)}</span>,
      value: (o) => label(o.status),
    },
    { header: 'Email', cell: (o) => o.email ?? '-', value: (o) => o.email },
    {
      header: 'Owner',
      cell: (o) =>
        o.ownerUserId ? (
          <>
            <div>{o.ownerName}</div>
            <div className="small text-muted">{o.ownerEmail}</div>
          </>
        ) : (
          <span className="text-muted">Not set</span>
        ),
      value: (o) => o.ownerName,
    },
    { header: 'Trial ends', cell: (o) => (o.trialEndsAt ? o.trialEndsAt.slice(0, 10) : '-') },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (o) => (
        <>
          <IconButton icon={<Eye size={15} />} label="View" className="me-1" onClick={() => setViewing(o)} />
          <IconButton icon={<UserCheck size={15} />} label="Set owner" className="me-1" onClick={() => setOwnerOf(o)} />
          <IconButton icon={<Edit2 size={15} />} label="Edit" className="me-1" onClick={() => setEditing(o)} />
          <IconButton
            icon={<Trash2 size={15} />}
            label="Delete"
            variant="danger"
            disabled={remove.isPending}
            onClick={() => confirm(`Delete ${o.name}? This removes its data.`) && remove.mutate(o.id)}
          />
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="Organizations" trail={['Platform']} />
      <Card
        title="Client organizations"
        actions={<AddButton onClick={() => setEditing('new')}>Add organization</AddButton>}
      >
        {error && <ErrorBox message={errorMessage(error)} />}
        {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
        <DataTable datatable columns={columns} rows={rows} loading={isLoading} empty="No organizations yet." />
      </Card>
      {viewing && (
        <OrganizationDetails
          org={viewing}
          onClose={() => setViewing(null)}
          onEdit={() => {
            // Swap the details dialog for the edit form of the same organization.
            setEditing(viewing)
            setViewing(null)
          }}
        />
      )}
      {editing && (
        <OrganizationForm
          item={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            refresh()
          }}
        />
      )}
      {ownerOf && (
        <OwnerModal
          org={ownerOf}
          onClose={() => setOwnerOf(null)}
          onSaved={() => {
            setOwnerOf(null)
            refresh()
          }}
        />
      )}
    </>
  )
}
