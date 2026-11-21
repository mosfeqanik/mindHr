import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Edit2, Eye, Trash2 } from 'react-feather'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { organizationsApi, rolesApi, usersApi } from '../../api/platform'
import type { User, UserType } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
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
import { MOBILE_NUMBER_RE, cleanPhone, label, numOrNull, orNull, userTypeLabel } from '../../lib/format'
import { AddButton, Card } from './shared'

// Super Admin accounts cannot be added; an existing Super Admin keeps its type (the select is locked).
const USER_TYPES: UserType[] = ['ADMIN', 'SAAS_STAFF', 'ORG_USER']

/** Built per form: a new user needs a password, an existing one keeps theirs when the field is empty. */
const makeSchema = (isNew: boolean) => z
  .object({
    fullName: z.string().trim().min(1, 'Full name is required').max(150),
    email: z.string().trim().min(1, 'Email is required').email('Email must be valid'),
    password: z.string().max(100),
    phone: z
        .string()
        .transform(cleanPhone)
        .refine((v) => v.length > 0, 'Mobile number is required')
        .refine((v) => v.length === 0 || MOBILE_NUMBER_RE.test(v), 'Enter a valid Bangladeshi mobile number, e.g. 01712345678'),
    userType: z.enum(['SUPER_ADMIN', 'ADMIN', 'SAAS_STAFF', 'ORG_USER']),
    organizationId: z.string(),
    roleIds: z.array(z.number()),
    active: z.boolean(),
  })
  .superRefine((v, ctx) => {
    if (isNew && !v.password) {
      ctx.addIssue({ code: 'custom', path: ['password'], message: 'Password is required for a new user' })
    } else if (v.password && v.password.length < 8) {
      ctx.addIssue({ code: 'custom', path: ['password'], message: 'Password must be at least 8 characters' })
    }
    if (v.userType === 'ORG_USER' && !v.organizationId) {
      ctx.addIssue({ code: 'custom', path: ['organizationId'], message: 'Organization is required for an organization user' })
    }
    if (v.roleIds.length !== 1) {
      ctx.addIssue({ code: 'custom', path: ['roleIds'], message: 'Choose a role' })
    }
  })
type Form = z.infer<ReturnType<typeof makeSchema>>

/**
 * A user holds exactly one role. Users saved before that rule may still have several: pre-pick the one that is
 * not the basic EMPLOYEE role (e.g. TEAM_LEAD over EMPLOYEE), so saving keeps their highest role.
 */
function initialRole(item: User | null): number[] {
  if (!item || item.roles.length === 0) return []
  const pick = item.roles.find((r) => r.name !== 'EMPLOYEE') ?? item.roles[0]
  return [pick.id]
}

function UserForm({ item, onClose, onSaved }: { item: User | null; onClose: () => void; onSaved: () => void }) {
  const { session } = useAuth()
  // Only the Super Admin may create or change platform accounts; the backend rejects it for SaaS staff.
  const userTypes: UserType[] = session?.userType === 'SUPER_ADMIN' ? USER_TYPES : ['ORG_USER']
  const orgs = useQuery({ queryKey: ['organizations', null], queryFn: () => organizationsApi.list() })
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(makeSchema(!item)),
    defaultValues: {
      fullName: item?.fullName ?? '',
      email: item?.email ?? '',
      password: '',
      phone: item?.phone ?? '',
      userType: item?.userType ?? 'ORG_USER',
      organizationId: item?.organizationId?.toString() ?? '',
      roleIds: initialRole(item),
      active: item?.active ?? true,
    },
  })

  const userType = watch('userType')
  const organizationId = watch('organizationId')
  const roleIds = watch('roleIds')
  const isOrgUser = userType === 'ORG_USER'
  const orgId = isOrgUser && organizationId ? Number(organizationId) : null

  // Platform users get platform roles (no organization); organization users get that organization's roles.
  const roles = useQuery({
    queryKey: ['roles', orgId ?? 'platform'],
    queryFn: () => rolesApi.list(orgId != null ? { organizationId: orgId } : undefined),
    enabled: !isOrgUser || orgId != null,
  })
  // The SUPER_ADMIN role unlocks Super Admin powers (plans, system settings), so only a Super Admin gets it;
  // Admins and mindHR Staff choose from the other platform roles.
  const roleOptions = (roles.data ?? []).filter(
    (r) => r.organizationId === orgId && !(userType !== 'SUPER_ADMIN' && r.name === 'SUPER_ADMIN'),
  )
  const platformRoleId = (name: string) =>
    (roles.data ?? []).find((r) => r.organizationId == null && r.name === name)?.id
  const superAdminRoleId = platformRoleId('SUPER_ADMIN')
  const adminRoleId = platformRoleId('ADMIN')

  // Changing the user's side or organization makes the old role choices invalid.
  const firstRender = useRef(true)
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false
      return
    }
    setValue('roleIds', [])
  }, [userType, organizationId, setValue])

  // A Super Admin needs the SUPER_ADMIN role for the pages that check it; an Admin gets the ADMIN role. Tick it.
  useEffect(() => {
    const preset = userType === 'SUPER_ADMIN' ? superAdminRoleId : userType === 'ADMIN' ? adminRoleId : undefined
    if (preset != null && roleIds.length === 0) {
      setValue('roleIds', [preset])
    }
  }, [userType, superAdminRoleId, adminRoleId, roleIds.length, setValue])

  const chooseRole = (id: number) => setValue('roleIds', [id], { shouldValidate: true })

  const onSubmit = async (v: Form) => {
    setError(null)
    const password = orNull(v.password)
    try {
      const body = {
        fullName: v.fullName.trim(),
        email: v.email.trim(),
        password,
        phone: v.phone,
        userType: v.userType,
        organizationId: v.userType === 'ORG_USER' ? numOrNull(v.organizationId) : null,
        active: v.active,
        // Exactly one role; PUT replaces whatever the user had.
        roleIds: v.roleIds,
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
              <Field label="Full name" required error={errors.fullName?.message}>
                <Input {...register('fullName')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Email" required error={errors.email?.message}>
                <Input type="email" {...register('email')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field
                label={item ? 'New password (leave empty to keep)' : 'Password'}
                required={!item}
                error={errors.password?.message}
              >
                <PasswordInput autoComplete="new-password" {...register('password')} />
              </Field>
            </div>
            <div className="col-md-6">
<Field label="Mobile number" required error={errors.phone?.message}>
                <Input type="tel" inputMode="tel" autoComplete="tel" placeholder="01XXXXXXXXX" {...register('phone')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="User type" required>
                {item?.userType === 'SUPER_ADMIN' ? (
                  // A Super Admin's type cannot change; the value stays in the form's defaults.
                  <Input value={userTypeLabel('SUPER_ADMIN')} readOnly />
                ) : (
                  <Select {...register('userType')}>
                    {userTypes.map((t) => (
                      <option key={t} value={t}>
                        {userTypeLabel(t)}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
            </div>
            {isOrgUser && (
              <div className="col-md-6">
                <Field label="Organization" required error={errors.organizationId?.message}>
                  <Select {...register('organizationId')}>
                    <option value="">Select organization...</option>
                    {(orgs.data ?? []).map((o) => (
                      <option key={o.id} value={o.id}>
                        {o.name}
                      </option>
                    ))}
                  </Select>
                </Field>
              </div>
            )}
            <div className="col-12">
              <Field label="Role" required error={errors.roleIds?.message}>
                {isOrgUser && orgId == null ? (
                  <div className="text-muted small">Choose an organization first to see its roles.</div>
                ) : roles.isLoading ? (
                  <div className="text-muted small">Loading roles...</div>
                ) : roleOptions.length === 0 ? (
                  <div className="text-muted small">No roles found.</div>
                ) : (
                  <div className="mh-role-grid" role="radiogroup" aria-label="Role">
                    {roleOptions.map((r) => (
                      <label key={r.id} className={`mh-role-option ${roleIds.includes(r.id) ? 'selected' : ''}`}>
                        <input
                          type="radio"
                          name="user-role"
                          className="form-check-input mh-square-choice"
                          checked={roleIds.includes(r.id)}
                          onChange={() => chooseRole(r.id)}
                        />
                        <span>
                          <span className="d-block text-dark">{label(r.name)}</span>
                          {r.description && <span className="d-block small text-muted">{r.description}</span>}
                        </span>
                      </label>
                    ))}
                  </div>
                )}
              </Field>
            </div>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" id="user-active" {...register('active')} />
            <label className="form-check-label" htmlFor="user-active">
              Active
            </label>
          </div>
          <div className="small text-muted mt-2">
            <span className="text-danger">*</span> Required
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
  /** Missing when the viewer may not edit users (only the Super Admin can). */
  onEdit?: () => void
}) {
  const rows: [string, React.ReactNode][] = [
    ['Full name', user.fullName],
    ['Email', user.email],
    ['Mobile number', user.phone ?? '-'],
    ['Type', userTypeLabel(user.userType)],
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
        {onEdit && (
          <Button onClick={onEdit}>
            <Edit2 size={15} className="me-1" />
            Edit
          </Button>
        )}
      </div>
    </Modal>
  )
}

export default function UsersPage() {
  // The Super Admin and Admins create, edit (including passwords) and delete users; mindHR staff can only view.
  // Only a Super Admin may touch a Super Admin account, and nobody deletes their own account.
  const { session } = useAuth()
  const canManage = session?.userType === 'SUPER_ADMIN' || session?.userType === 'ADMIN'
  const canEditUser = (u: User) => canManage && (u.userType !== 'SUPER_ADMIN' || session?.userType === 'SUPER_ADMIN')
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
    { header: 'Type', cell: (u) => userTypeLabel(u.userType), value: (u) => userTypeLabel(u.userType) },
    { header: 'Organization', cell: (u) => orgName(u.organizationId) },
    {
      header: 'Roles',
      cell: (u) => (u.roles.length ? u.roles.map((r) => label(r.name)).join(', ') : '-'),
      value: (u) => u.roles.map((r) => r.name).join(', '),
    },
    { header: 'Status', cell: (u) => <ActiveBadge active={u.active} />, value: (u) => (u.active ? 'Active' : 'Inactive') },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (u) => (
        <>
          <IconButton icon={<Eye size={15} />} label="View" className="me-1" onClick={() => setViewing(u)} />
          {canEditUser(u) && (
            <>
              <IconButton icon={<Edit2 size={15} />} label="Edit" className="me-1" onClick={() => setEditing(u)} />
              {u.id !== session?.userId && (
                <IconButton
                  icon={<Trash2 size={15} />}
                  label="Delete"
                  variant="danger"
                  disabled={remove.isPending}
                  onClick={() => confirm(`Delete ${u.fullName}?`) && remove.mutate(u.id)}
                />
              )}
            </>
          )}
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
            {canManage && <AddButton onClick={() => setEditing('new')}>Add user</AddButton>}
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
          onEdit={
            canEditUser(viewing)
              ? () => {
                  // Swap the details dialog for the edit form of the same user.
                  setEditing(viewing)
                  setViewing(null)
                }
              : undefined
          }
        />
      )}
      {editing && canManage && (
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
