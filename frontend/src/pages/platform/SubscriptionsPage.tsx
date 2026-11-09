import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { subscriptionPlansApi, subscriptionsApi } from '../../api/platform'
import {
  CHANNEL_LABEL,
  PAYMENT_CHANNELS,
  SUBSCRIPTION_STATUSES,
  type PaymentChannel,
  type Subscription,
  type SubscriptionPaymentResult,
  type SubscriptionStatus,
} from '../../api/types'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, PageHeader, Select, type Column } from '../../components/ui'
import { formatMoney, label, orNull } from '../../lib/format'
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

/** Take one month of the current plan price; the backend creates invoice, payment and receipt in one go. */
function CollectPaymentModal({ item, onClose, onDone }: { item: Subscription; onClose: () => void; onDone: () => void }) {
  const quote = useQuery({ queryKey: ['subscription-quote', item.id], queryFn: () => subscriptionsApi.paymentQuote(item.id) })
  const [method, setMethod] = useState<PaymentChannel>('CASH')
  const [transactionId, setTransactionId] = useState('')
  const [notes, setNotes] = useState('')
  const [result, setResult] = useState<SubscriptionPaymentResult | null>(null)
  const save = useMutation({
    mutationFn: () =>
      subscriptionsApi.collectPayment(item.id, { method, transactionId: orNull(transactionId), notes: orNull(notes) }),
    onSuccess: setResult,
  })
  const needsTrx = method === 'BKASH' && transactionId.trim() === ''
  const q = quote.data

  if (result) {
    return (
      <Modal title="Payment received" onClose={onDone}>
        <div className="modal-body">
          <p className="mb-2">
            {formatMoney(result.payment.amount)} {result.payment.currency} by {CHANNEL_LABEL[method]} for{' '}
            {result.periodStart} to {result.periodEnd}.
          </p>
          <ul className="small text-muted mb-0">
            <li>Invoice {result.invoice.invoiceNumber} (paid)</li>
            <li>Receipt {result.receipt.receiptNumber}</li>
            <li>Next renewal: {result.subscription.renewalDate}</li>
          </ul>
        </div>
        <div className="modal-footer">
          <Button onClick={onDone}>Done</Button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title="Collect monthly payment" onClose={onClose}>
      <div className="modal-body">
        {quote.error && <ErrorBox message={errorMessage(quote.error)} />}
        {save.error && <ErrorBox message={errorMessage(save.error)} />}
        {q ? (
          <div className="alert alert-light border">
            <div className="text-muted small">{q.planName} &middot; 1 month</div>
            <div className="fs-4 text-dark">
              {formatMoney(q.amount)} {q.currency}
            </div>
            <div className="small">
              Covers {q.periodStart} to {q.periodEnd}
            </div>
          </div>
        ) : (
          !quote.error && <p className="text-muted">Loading amount...</p>
        )}
        <Field label="Payment method">
          <Select value={method} onChange={(e) => setMethod(e.target.value as PaymentChannel)}>
            {PAYMENT_CHANNELS.map((m) => (
              <option key={m} value={m}>
                {CHANNEL_LABEL[m]}
              </option>
            ))}
          </Select>
        </Field>
        {method !== 'CASH' && (
          <Field label={method === 'BKASH' ? 'bKash TrxID' : 'Card approval / reference (optional)'}>
            <Input value={transactionId} onChange={(e) => setTransactionId(e.target.value)} maxLength={100} />
          </Field>
        )}
        <Field label="Note (optional)">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={!q || needsTrx || save.isPending} onClick={() => save.mutate()}>
          {save.isPending ? 'Saving...' : q ? `Receive ${formatMoney(q.amount)} ${q.currency}` : 'Receive'}
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

type Action = { kind: 'edit' | 'status' | 'plan' | 'history' | 'pay'; item: Subscription } | { kind: 'new' }

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
          {s.status !== 'CANCELLED' && (
            <Button size="sm" className="me-2" onClick={() => setAction({ kind: 'pay', item: s })}>
              Collect payment
            </Button>
          )}
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
      {action?.kind === 'pay' && (
        <CollectPaymentModal
          item={action.item}
          onClose={() => setAction(null)}
          onDone={() => {
            done()
            for (const key of ['invoices', 'payments', 'receipts']) qc.invalidateQueries({ queryKey: [key] })
          }}
        />
      )}
      {action?.kind === 'history' && <SubscriptionHistory item={action.item} onClose={() => setAction(null)} />}
    </>
  )
}
