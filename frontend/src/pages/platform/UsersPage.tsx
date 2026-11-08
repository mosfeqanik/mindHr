import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Edit2, Eye, Trash2 } from 'react-feather'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { organizationsApi, usersApi } from '../../api/platform'
import type { User, UserType } from '../../api/types'
import { useResource } from '../../api/useResource'
import {
  ActiveBadge,
  Button,
  DataTable,
  IconButton,
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
import { label, numOrNull, orNull } from '../../lib/format'
import { AddButton, Card } from './shared'

const USER_TYPES: UserType[] = ['SUPER_ADMIN', 'SAAS_STAFF', 'ORG_USER']

const schema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150),
  email: z.string().min(1, 'Email is required').email('Email must be valid'),
  password: z.string().max(100),
  phone: z.string().max(30),
  userType: z.enum(['SUPER_ADMIN', 'SAAS_STAFF', 'ORG_USER']),
  organizationId: z.string(),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

function UserForm({ item, onClose, onSaved }: { item: User | null; onClose: () => void; onSaved: () => void }) {
  const orgs = useQuery({ queryKey: ['organizations', null], queryFn: () => organizationsApi.list() })
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      fullName: item?.fullName ?? '',
      email: item?.email ?? '',
      password: '',
      phone: item?.phone ?? '',
      userType: item?.userType ?? 'ORG_USER',
      organizationId: item?.organizationId?.toString() ?? '',
      active: item?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const password = orNull(v.password)
    if (!item && !password) {
      setFieldError('password', { message: 'Password is required for a new user' })
      return
    }
    if (password && password.length < 8) {
      setFieldError('password', { message: 'Password must be at least 8 characters' })
      return
    }
    try {
      const body = {
        fullName: v.fullName.trim(),
        email: v.email.trim(),
        password,
        phone: orNull(v.phone),
        userType: v.userType,
        organizationId: v.userType === 'ORG_USER' ? numOrNull(v.organizationId) : null,
        active: v.active,
        // PUT replaces roles, so send the current ones back.
        roleIds: item?.roles.map((r) => r.id),
      }
      if (item) await usersApi.update(item.id, body)
      else await usersApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit user' : 'Add user'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-6">
              <Field label="Full name" error={errors.fullName?.message}>
                <Input {...register('fullName')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Email" error={errors.email?.message}>
                <Input type="email" {...register('email')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label={item ? 'New password (leave empty to keep)' : 'Password'} error={errors.password?.message}>
                <PasswordInput autoComplete="new-password" {...register('password')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Phone" error={errors.phone?.message}>
                <Input {...register('phone')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="User type">
                <Select {...register('userType')}>
                  {USER_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {label(t)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Organization (org users)">
                <Select {...register('organizationId')}>
                  <option value="">-</option>
                  {(orgs.data ?? []).map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.name}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="user-active" {...register('active')} />
            <label className="form-check-label" htmlFor="user-active">
              Active
            </label>
          </div>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

const formatDateTime = (v: string | null | undefined) => (v ? v.slice(0, 16).replace('T', ' ') : '-')

/** Read-only details of a user; the table shows only the main columns. */
function UserDetails({
  user,
  orgName,
  onClose,
  onEdit,
}: {
  user: User
  orgName: string
  onClose: () => void
  onEdit: () => void
}) {
  const rows: [string, React.ReactNode][] = [
    ['Full name', user.fullName],
    ['Email', user.email],
    ['Phone', user.phone ?? '-'],
    ['Type', label(user.userType)],
    ['Organization', orgName],
    ['Roles', user.roles.length ? user.roles.map((r) => r.name).join(', ') : '-'],
    ['Status', <ActiveBadge active={user.active} />],
    ['Last login', formatDateTime(user.lastLoginAt)],
  ]
  return (
    <Modal title={user.fullName} onClose={onClose}>
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

export default function UsersPage() {
  const [organizationId, setOrganizationId] = useState('')
  const orgs = useQuery({ queryKey: ['organizations', null], queryFn: () => organizationsApi.list() })
  const orgName = (id: number | null) => (id == null ? 'Platform' : (orgs.data?.find((o) => o.id === id)?.name ?? `#${id}`))

  const { rows, isLoading, error, remove, refresh } = useResource(
    'users',
    usersApi,
    organizationId ? { organizationId: Number(organizationId) } : undefined,
  )
  const [editing, setEditing] = useState<User | 'new' | null>(null)
  const [viewing, setViewing] = useState<User | null>(null)

  const columns: Column<User>[] = [
    { header: 'Name', cell: (u) => <span className="text-dark font-weight-medium">{u.fullName}</span>, value: (u) => u.fullName },
    { header: 'Organization', cell: (u) => orgName(u.organizationId) },
    { header: 'Roles', cell: (u) => (u.roles.length ? u.roles.map((r) => r.name).join(', ') : '-') },
    { header: 'Status', cell: (u) => <ActiveBadge active={u.active} />, value: (u) => (u.active ? 'Active' : 'Inactive') },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (u) => (
        <>
          <IconButton icon={<Eye size={15} />} label="View" className="me-1" onClick={() => setViewing(u)} />
          <IconButton icon={<Edit2 size={15} />} label="Edit" className="me-1" onClick={() => setEditing(u)} />
          <IconButton
            icon={<Trash2 size={15} />}
            label="Delete"
            variant="danger"
            disabled={remove.isPending}
            onClick={() => confirm(`Delete ${u.fullName}?`) && remove.mutate(u.id)}
          />
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="Users" trail={['Platform']} />
      <Card
        title="All users"
        actions={
          <>
            <select className="form-select w-auto" value={organizationId} onChange={(e) => setOrganizationId(e.target.value)}>
              <option value="">All organizations</option>
              {(orgs.data ?? []).map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </select>
            <AddButton onClick={() => setEditing('new')}>Add user</AddButton>
          </>
        }
      >
        {error && <ErrorBox message={errorMessage(error)} />}
        {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
        <DataTable datatable columns={columns} rows={rows} loading={isLoading} empty="No users found." />
      </Card>
      {viewing && (
        <UserDetails
          user={viewing}
          orgName={orgName(viewing.organizationId)}
          onClose={() => setViewing(null)}
          onEdit={() => {
            // Swap the details dialog for the edit form of the same user.
            setEditing(viewing)
            setViewing(null)
          }}
        />
      )}
      {editing && (
        <UserForm
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
