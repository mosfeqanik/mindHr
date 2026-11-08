import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { supportTicketsApi, usersApi } from '../../api/platform'
import {
  TICKET_PRIORITIES,
  TICKET_STATUSES,
  TICKET_TYPES,
  type SupportTicket,
  type TicketPriority,
  type TicketStatus,
} from '../../api/types'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, PageHeader, Select, type Column } from '../../components/ui'
import { label } from '../../lib/format'
import { Badge, Card, StatusSelect } from './shared'

const PRIORITY_BADGE: Record<TicketPriority, string> = {
  LOW: 'bg-secondary',
  MEDIUM: 'bg-info',
  HIGH: 'bg-warning text-dark',
  URGENT: 'bg-danger',
}

const schema = z.object({
  subject: z.string().min(1, 'Subject is required').max(200),
  description: z.string().min(1, 'Description is required'),
  type: z.enum(TICKET_TYPES),
  priority: z.enum(TICKET_PRIORITIES),
})
type Form = z.infer<typeof schema>

function TicketForm({ item, onClose, onSaved }: { item: SupportTicket | null; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      subject: item?.subject ?? '',
      description: item?.description ?? '',
      type: item?.type ?? 'REQUEST',
      priority: item?.priority ?? 'MEDIUM',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = { ...v, subject: v.subject.trim(), description: v.description.trim() }
    try {
      if (item) await supportTicketsApi.update(item.id, body)
      else await supportTicketsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? `Edit ${item.ticketNumber}` : 'New ticket'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Subject" error={errors.subject?.message}>
            <Input {...register('subject')} />
          </Field>
          <div className="row">
            <div className="col-md-6">
              <Field label="Type">
                <Select {...register('type')}>
                  {TICKET_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {label(t)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Priority">
                <Select {...register('priority')}>
                  {TICKET_PRIORITIES.map((p) => (
                    <option key={p} value={p}>
                      {label(p)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          </div>
          <Field label="Description" error={errors.description?.message}>
            <textarea className="form-control" rows={5} {...register('description')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

/** Assign to a SaaS-side user (support staff or the super admin). */
function AssignModal({ item, onClose, onDone }: { item: SupportTicket; onClose: () => void; onDone: () => void }) {
  const users = useQuery({ queryKey: ['users', null], queryFn: () => usersApi.list() })
  const staff = (users.data ?? []).filter((u) => u.userType !== 'ORG_USER' && u.active)
  const [userId, setUserId] = useState(item.assignedToUserId?.toString() ?? '')
  const save = useMutation({ mutationFn: () => supportTicketsApi.assign(item.id, Number(userId)), onSuccess: onDone })

  return (
    <Modal title={`Assign ${item.ticketNumber}`} onClose={onClose}>
      <div className="modal-body">
        {save.error && <ErrorBox message={errorMessage(save.error)} />}
        {users.error && <ErrorBox message={errorMessage(users.error)} />}
        <Field label="Assign to">
          <Select value={userId} onChange={(e) => setUserId(e.target.value)}>
            <option value="">Select...</option>
            {staff.map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName} ({label(u.userType)})
              </option>
            ))}
          </Select>
        </Field>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={!userId || save.isPending} onClick={() => save.mutate()}>
          {save.isPending ? 'Saving...' : 'Assign'}
        </Button>
      </div>
    </Modal>
  )
}

function CommentsModal({ item, onClose }: { item: SupportTicket; onClose: () => void }) {
  const qc = useQueryClient()
  const key = ['ticket-comments', item.id]
  const { data, isLoading, error } = useQuery({ queryKey: key, queryFn: () => supportTicketsApi.comments(item.id) })
  const [body, setBody] = useState('')
  const [internal, setInternal] = useState(false)
  const add = useMutation({
    mutationFn: () => supportTicketsApi.addComment(item.id, body.trim(), internal),
    onSuccess: () => {
      setBody('')
      qc.invalidateQueries({ queryKey: key })
    },
  })

  return (
    <Modal title={`${item.ticketNumber}: ${item.subject}`} onClose={onClose}>
      <div className="modal-body">
        <p className="text-dark" style={{ whiteSpace: 'pre-wrap' }}>
          {item.description}
        </p>
        <small className="text-muted">Opened by {item.createdByName ?? 'unknown'}</small>
        <hr />
        {error && <ErrorBox message={errorMessage(error)} />}
        {isLoading && <p className="text-muted">Loading...</p>}
        {!isLoading && (data ?? []).length === 0 && <p className="text-muted">No comments yet.</p>}
        <ul className="list-unstyled">
          {(data ?? []).map((c) => (
            <li key={c.id} className={`border rounded p-2 mb-2 ${c.internal ? 'bg-light' : ''}`}>
              <div className="small text-muted mb-1">
                {c.authorName ?? 'Unknown'}
                {c.internal && <span className="badge bg-secondary ms-2">Internal</span>}
              </div>
              <div style={{ whiteSpace: 'pre-wrap' }}>{c.body}</div>
            </li>
          ))}
        </ul>
        {add.error && <ErrorBox message={errorMessage(add.error)} />}
        <Field label="Add a comment">
          <textarea className="form-control" rows={3} value={body} onChange={(e) => setBody(e.target.value)} />
        </Field>
        <label className="form-check">
          <input
            type="checkbox"
            className="form-check-input"
            checked={internal}
            onChange={(e) => setInternal(e.target.checked)}
          />
          <span className="form-check-label">Internal note (staff only, the client does not see it)</span>
        </label>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
        <Button disabled={!body.trim() || add.isPending} onClick={() => add.mutate()}>
          {add.isPending ? 'Posting...' : 'Post comment'}
        </Button>
      </div>
    </Modal>
  )
}

type Action = { kind: 'edit' | 'assign' | 'comments'; item: SupportTicket } | { kind: 'new' }

export default function SupportTicketsPage() {
  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['support-tickets'], queryFn: () => supportTicketsApi.list() })
  const refresh = () => qc.invalidateQueries({ queryKey: ['support-tickets'] })
  const [filter, setFilter] = useState<TicketStatus | ''>('')
  const [action, setAction] = useState<Action | null>(null)
  const status = useMutation({
    mutationFn: ({ id, value }: { id: number; value: TicketStatus }) => supportTicketsApi.changeStatus(id, value),
    onSuccess: refresh,
  })
  const remove = useMutation({ mutationFn: (id: number) => supportTicketsApi.remove(id), onSuccess: refresh })
  const done = () => {
    setAction(null)
    refresh()
  }

  const rows = (data ?? []).filter((t) => !filter || t.status === filter)

  const columns: Column<SupportTicket>[] = [
    { header: 'Ticket', cell: (t) => t.ticketNumber },
    {
      header: 'Subject',
      cell: (t) => (
        <a role="button" className="text-dark font-weight-medium" onClick={() => setAction({ kind: 'comments', item: t })}>
          {t.subject}
        </a>
      ),
    },
    { header: 'Type', cell: (t) => label(t.type) },
    { header: 'Priority', cell: (t) => <Badge value={t.priority} classes={PRIORITY_BADGE} /> },
    { header: 'Assigned to', cell: (t) => t.assignedToName ?? <span className="text-muted">Nobody</span> },
    {
      header: 'Status',
      cell: (t) => (
        <StatusSelect
          value={t.status}
          options={TICKET_STATUSES}
          disabled={status.isPending}
          onChange={(value) => status.mutate({ id: t.id, value })}
        />
      ),
    },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (t) => (
        <>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'comments', item: t })}>
            Comments
          </Button>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'assign', item: t })}>
            Assign
          </Button>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'edit', item: t })}>
            Edit
          </Button>
          <Button
            variant="danger"
            size="sm"
            disabled={remove.isPending}
            onClick={() => confirm(`Delete ticket ${t.ticketNumber}?`) && remove.mutate(t.id)}
          >
            Delete
          </Button>
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="Support Tickets"
        trail={['Client account']}
        actions={<Button onClick={() => setAction({ kind: 'new' })}>New ticket</Button>}
      />
      <Card title="Tickets">
        <div className="mb-2">
          <select
            className="form-select w-auto"
            value={filter}
            onChange={(e) => setFilter(e.target.value as TicketStatus | '')}
          >
            <option value="">All statuses</option>
            {TICKET_STATUSES.map((s) => (
              <option key={s} value={s}>
                {label(s)}
              </option>
            ))}
          </select>
        </div>
        {error && <ErrorBox message={errorMessage(error)} />}
        {status.error && <ErrorBox message={errorMessage(status.error)} />}
        {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
        <DataTable columns={columns} rows={rows} loading={isLoading} empty="No tickets." />
      </Card>
      {action?.kind === 'new' && <TicketForm item={null} onClose={() => setAction(null)} onSaved={done} />}
      {action?.kind === 'edit' && <TicketForm item={action.item} onClose={() => setAction(null)} onSaved={done} />}
      {action?.kind === 'assign' && <AssignModal item={action.item} onClose={() => setAction(null)} onDone={done} />}
      {action?.kind === 'comments' && <CommentsModal item={action.item} onClose={() => setAction(null)} />}
    </>
  )
}
