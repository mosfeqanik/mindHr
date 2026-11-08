import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { subscriptionPlansApi, subscriptionsApi } from '../../api/platform'
import { SUBSCRIPTION_STATUSES, type Subscription, type SubscriptionStatus } from '../../api/types'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, PageHeader, Select, type Column } from '../../components/ui'
import { label, orNull } from '../../lib/format'
import { Badge, Card, HistoryModal, StatusModal } from './shared'

const STATUS_BADGE: Record<SubscriptionStatus, string> = {
  TRIAL: 'bg-info',
  ACTIVE: 'bg-success',
  PAYMENT_FAILED: 'bg-danger',
  GRACE_PERIOD: 'bg-warning text-dark',
  CANCELLED: 'bg-secondary',
  EXPIRED: 'bg-secondary',
  SUSPENDED: 'bg-danger',
}

function usePlans() {
  const { data } = useQuery({ queryKey: ['subscription-plans', null], queryFn: () => subscriptionPlansApi.list() })
  return data ?? []
}

const schema = z.object({
  planId: z.string().min(1, 'Plan is required'),
  status: z.enum(SUBSCRIPTION_STATUSES),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string(),
  renewalDate: z.string(),
  autoRenew: z.boolean(),
})
type Form = z.infer<typeof schema>

/** New subscriptions pick plan and status; later changes go through the Plan / Status actions. */
function SubscriptionForm({ item, onClose, onSaved }: { item: Subscription | null; onClose: () => void; onSaved: () => void }) {
  const plans = usePlans()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      planId: item?.planId.toString() ?? '',
      status: item?.status ?? 'TRIAL',
      startDate: item?.startDate ?? new Date().toISOString().slice(0, 10),
      endDate: item?.endDate ?? '',
      renewalDate: item?.renewalDate ?? '',
      autoRenew: item?.autoRenew ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const dates = { endDate: orNull(v.endDate), renewalDate: orNull(v.renewalDate), autoRenew: v.autoRenew }
    try {
      if (item) await subscriptionsApi.update(item.id, dates)
      else await subscriptionsApi.create({ ...dates, planId: Number(v.planId), status: v.status, startDate: v.startDate })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit subscription' : 'Add subscription'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            {item ? (
              <p className="text-muted small">
                {item.planName} &middot; {label(item.status)} &middot; started {item.startDate}. Use the Plan and Status
                buttons to change those.
              </p>
            ) : (
              <>
                <div className="col-md-6">
                  <Field label="Plan" error={errors.planId?.message}>
                    <Select {...register('planId')}>
                      <option value="">Select...</option>
                      {plans.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                    </Select>
                  </Field>
                </div>
                <div className="col-md-6">
                  <Field label="Status">
                    <Select {...register('status')}>
                      {SUBSCRIPTION_STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {label(s)}
                        </option>
                      ))}
                    </Select>
                  </Field>
                </div>
              </>
            )}
            <div className="col-md-4">
              <Field label="Start date" error={errors.startDate?.message}>
                {item ? <Input value={item.startDate} readOnly /> : <Input type="date" {...register('startDate')} />}
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="End date">
                <Input type="date" {...register('endDate')} />
              </Field>
            </div>
            <div className="col-md-4">
              <Field label="Renewal date">
                <Input type="date" {...register('renewalDate')} />
              </Field>
            </div>
          </div>
          <label className="form-check">
            <input type="checkbox" className="form-check-input" {...register('autoRenew')} />
            <span className="form-check-label">Renew automatically</span>
          </label>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

function PlanModal({ item, onClose, onDone }: { item: Subscription; onClose: () => void; onDone: () => void }) {
  const plans = usePlans()
  const [planId, setPlanId] = useState(item.planId.toString())
  const [note, setNote] = useState('')
  const save = useMutation({
    mutationFn: () => subscriptionsApi.changePlan(item.id, Number(planId), orNull(note)),
    onSuccess: onDone,
  })

  return (
    <Modal title="Change plan" onClose={onClose}>
      <div className="modal-body">
        {save.error && <ErrorBox message={errorMessage(save.error)} />}
        <Field label="Plan">
          <Select value={planId} onChange={(e) => setPlanId(e.target.value)}>
            {plans.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Note (optional)">
          <Input value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={planId === item.planId.toString() || save.isPending} onClick={() => save.mutate()}>
          {save.isPending ? 'Saving...' : 'Change plan'}
        </Button>
      </div>
    </Modal>
  )
}

function SubscriptionHistory({ item, onClose }: { item: Subscription; onClose: () => void }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['subscription-history', item.id],
    queryFn: () => subscriptionsApi.history(item.id),
  })
  const rows = (data ?? []).map((h) => ({
    ...h,
    change:
      h.fromPlanName !== h.toPlanName
        ? `Plan: ${h.fromPlanName ?? '-'} → ${h.toPlanName ?? '-'}`
        : `Status: ${label(h.fromStatus)} → ${label(h.toStatus)}`,
  }))
  return <HistoryModal title="Subscription history" rows={rows} loading={isLoading} error={error} onClose={onClose} />
}

type Action = { kind: 'edit' | 'status' | 'plan' | 'history'; item: Subscription } | { kind: 'new' }

export default function SubscriptionsPage() {
  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['subscriptions'], queryFn: () => subscriptionsApi.list() })
  const refresh = () => qc.invalidateQueries({ queryKey: ['subscriptions'] })
  const [action, setAction] = useState<Action | null>(null)
  const done = () => {
    setAction(null)
    refresh()
  }

  const columns: Column<Subscription>[] = [
    { header: 'Plan', cell: (s) => <span className="text-dark font-weight-medium">{s.planName}</span> },
    { header: 'Status', cell: (s) => <Badge value={s.status} classes={STATUS_BADGE} /> },
    { header: 'Start', cell: (s) => s.startDate },
    { header: 'End', cell: (s) => s.endDate ?? '-' },
    { header: 'Renewal', cell: (s) => s.renewalDate ?? '-' },
    { header: 'Auto renew', cell: (s) => (s.autoRenew ? 'Yes' : 'No') },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (s) => (
        <>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'status', item: s })}>
            Status
          </Button>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'plan', item: s })}>
            Plan
          </Button>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'edit', item: s })}>
            Edit
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setAction({ kind: 'history', item: s })}>
            History
          </Button>
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="Subscriptions"
        trail={['Client account']}
        actions={<Button onClick={() => setAction({ kind: 'new' })}>Add subscription</Button>}
      />
      <Card title="Subscriptions">
        {error && <ErrorBox message={errorMessage(error)} />}
        <DataTable columns={columns} rows={data ?? []} loading={isLoading} empty="No subscriptions yet." />
      </Card>
      {action?.kind === 'new' && <SubscriptionForm item={null} onClose={() => setAction(null)} onSaved={done} />}
      {action?.kind === 'edit' && <SubscriptionForm item={action.item} onClose={() => setAction(null)} onSaved={done} />}
      {action?.kind === 'plan' && <PlanModal item={action.item} onClose={() => setAction(null)} onDone={done} />}
      {action?.kind === 'status' && (
        <StatusModal
          title="Change subscription status"
          current={action.item.status}
          options={SUBSCRIPTION_STATUSES}
          onClose={() => setAction(null)}
          onSave={(status, note) => subscriptionsApi.changeStatus(action.item.id, status, note)}
          onDone={done}
        />
      )}
      {action?.kind === 'history' && <SubscriptionHistory item={action.item} onClose={() => setAction(null)} />}
    </>
  )
}
