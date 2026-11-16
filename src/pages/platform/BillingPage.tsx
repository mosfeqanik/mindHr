import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { invoicesApi, paymentsApi, subscriptionsApi } from '../../api/platform'
import {
  INVOICE_STATUSES,
  PAYMENT_STATUSES,
  type Invoice,
  type InvoiceStatus,
  type Payment,
  type PaymentStatus,
} from '../../api/types'
import {
  Button,
  DataTable,
  ErrorBox,
  Field,
  Input,
  Modal,
  ModalFooter,
  PageHeader,
  Select,
  Tabs,
  type Column,
} from '../../components/ui'
import { formatMoney, numOrNull, orNull } from '../../lib/format'
import { Card, StatusSelect } from './shared'

const amount = z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter an amount, e.g. 49.99')
const currency = z.union([z.literal(''), z.string().length(3, '3-letter code')])
const money = (v: number, c: string | null) => `${formatMoney(v)} ${c ?? ''}`

// ---- Invoices ----

const invoiceSchema = z
  .object({
    invoiceNumber: z.string().min(1, 'Invoice number is required').max(30),
    subscriptionId: z.string(),
    amount,
    currency,
    issueDate: z.string().min(1, 'Issue date is required'),
    dueDate: z.string().min(1, 'Due date is required'),
    notes: z.string(),
  })
  .refine((v) => !v.issueDate || !v.dueDate || v.dueDate >= v.issueDate, {
    path: ['dueDate'],
    message: 'Due date must not be before issue date',
  })
type InvoiceForm = z.infer<typeof invoiceSchema>

function InvoiceModal({ item, onClose, onSaved }: { item: Invoice | null; onClose: () => void; onSaved: () => void }) {
  const subs = useQuery({ queryKey: ['subscriptions'], queryFn: () => subscriptionsApi.list() })
  const [error, setError] = useState<string | null>(null)
  const today = new Date().toISOString().slice(0, 10)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<InvoiceForm>({
    resolver: zodResolver(invoiceSchema),
    defaultValues: {
      invoiceNumber: item?.invoiceNumber ?? '',
      subscriptionId: item?.subscriptionId?.toString() ?? '',
      amount: item?.amount.toString() ?? '',
      currency: item?.currency ?? 'USD',
      issueDate: item?.issueDate ?? today,
      dueDate: item?.dueDate ?? '',
      notes: item?.notes ?? '',
    },
  })

  const onSubmit = async (v: InvoiceForm) => {
    setError(null)
    const body = {
      invoiceNumber: v.invoiceNumber.trim(),
      subscriptionId: numOrNull(v.subscriptionId),
      amount: Number(v.amount),
      currency: orNull(v.currency.toUpperCase()),
      issueDate: v.issueDate,
      dueDate: v.dueDate,
      notes: orNull(v.notes),
    }
    try {
      if (item) await invoicesApi.update(item.id, body)
      else await invoicesApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit invoice' : 'Add invoice'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-6">
              <Field label="Invoice number" error={errors.invoiceNumber?.message}>
                <Input {...register('invoiceNumber')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Subscription (optional)">
                <Select {...register('subscriptionId')}>
                  <option value="">None</option>
                  {(subs.data ?? []).map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.planName} (from {s.startDate})
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Amount" error={errors.amount?.message}>
                <Input inputMode="decimal" {...register('amount')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Currency" error={errors.currency?.message}>
                <Input maxLength={3} {...register('currency')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Issue date" error={errors.issueDate?.message}>
                <Input type="date" {...register('issueDate')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Due date" error={errors.dueDate?.message}>
                <Input type="date" {...register('dueDate')} />
              </Field>
            </div>
          </div>
          <Field label="Notes">
            <Input {...register('notes')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

function InvoicesTab() {
  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['invoices'], queryFn: () => invoicesApi.list() })
  const refresh = () => qc.invalidateQueries({ queryKey: ['invoices'] })
  const [editing, setEditing] = useState<Invoice | 'new' | null>(null)
  const status = useMutation({
    mutationFn: ({ id, value }: { id: number; value: InvoiceStatus }) => invoicesApi.changeStatus(id, value),
    onSuccess: refresh,
  })

  const columns: Column<Invoice>[] = [
    { header: 'Number', cell: (i) => <span className="text-dark font-weight-medium">{i.invoiceNumber}</span> },
    { header: 'Amount', cell: (i) => money(i.amount, i.currency) },
    { header: 'Issued', cell: (i) => i.issueDate },
    { header: 'Due', cell: (i) => i.dueDate },
    { header: 'Paid', cell: (i) => (i.paidAt ? i.paidAt.slice(0, 10) : '-') },
    {
      header: 'Status',
      cell: (i) => (
        <StatusSelect
          value={i.status}
          options={INVOICE_STATUSES}
          disabled={status.isPending}
          onChange={(value) => status.mutate({ id: i.id, value })}
        />
      ),
    },
    {
      header: '',
      className: 'text-end',
      cell: (i) => (
        <Button variant="secondary" size="sm" onClick={() => setEditing(i)}>
          Edit
        </Button>
      ),
    },
  ]

  return (
    <>
      <div className="d-flex justify-content-end mb-2">
        <Button onClick={() => setEditing('new')}>Add invoice</Button>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {status.error && <ErrorBox message={errorMessage(status.error)} />}
      <DataTable columns={columns} rows={data ?? []} loading={isLoading} empty="No invoices yet." />
      {editing && (
        <InvoiceModal
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

// ---- Payments ----

const paymentSchema = z.object({
  invoiceId: z.string().min(1, 'Invoice is required'),
  amount,
  currency,
  gateway: z.string().max(50),
  gatewayTransactionId: z.string().max(100),
})
type PaymentForm = z.infer<typeof paymentSchema>

function PaymentModal({ invoices, onClose, onSaved }: { invoices: Invoice[]; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<PaymentForm>({
    resolver: zodResolver(paymentSchema),
    defaultValues: { invoiceId: '', amount: '', currency: 'USD', gateway: '', gatewayTransactionId: '' },
  })

  const onSubmit = async (v: PaymentForm) => {
    setError(null)
    try {
      await paymentsApi.create({
        invoiceId: Number(v.invoiceId),
        amount: Number(v.amount),
        currency: orNull(v.currency.toUpperCase()),
        gateway: orNull(v.gateway),
        gatewayTransactionId: orNull(v.gatewayTransactionId),
      })
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title="Record payment" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <Field label="Invoice" error={errors.invoiceId?.message}>
            <Select
              {...register('invoiceId', {
                // Prefill the amount and currency from the chosen invoice.
                onChange: (e) => {
                  const inv = invoices.find((i) => i.id === Number(e.target.value))
                  if (inv) {
                    setValue('amount', inv.amount.toString())
                    setValue('currency', inv.currency ?? '')
                  }
                },
              })}
            >
              <option value="">Select...</option>
              {invoices.map((i) => (
                <option key={i.id} value={i.id}>
                  {i.invoiceNumber} &middot; {money(i.amount, i.currency)} &middot; {i.status}
                </option>
              ))}
            </Select>
          </Field>
          <div className="row">
            <div className="col-md-6">
              <Field label="Amount" error={errors.amount?.message}>
                <Input inputMode="decimal" {...register('amount')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Currency" error={errors.currency?.message}>
                <Input maxLength={3} {...register('currency')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Gateway" error={errors.gateway?.message}>
                <Input placeholder="e.g. Stripe, bank transfer" {...register('gateway')} />
              </Field>
            </div>
            <div className="col-md-6">
              <Field label="Transaction ID" error={errors.gatewayTransactionId?.message}>
                <Input {...register('gatewayTransactionId')} />
              </Field>
            </div>
          </div>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

function PaymentsTab() {
  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['payments'], queryFn: () => paymentsApi.list() })
  const invoices = useQuery({ queryKey: ['invoices'], queryFn: () => invoicesApi.list() })
  // A payment can mark its invoice as paid, so refresh both.
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['payments'] })
    qc.invalidateQueries({ queryKey: ['invoices'] })
  }
  const [adding, setAdding] = useState(false)
  const [receipt, setReceipt] = useState<string | null>(null)
  const status = useMutation({
    mutationFn: ({ id, value }: { id: number; value: PaymentStatus }) => paymentsApi.changeStatus(id, value),
    onSuccess: refresh,
  })
  const issue = useMutation({
    mutationFn: (id: number) => paymentsApi.issueReceipt(id),
    onSuccess: (r) => {
      setReceipt(`Receipt ${r.receiptNumber} issued.`)
      refresh()
    },
  })
  const invoiceNumber = (id: number) => invoices.data?.find((i) => i.id === id)?.invoiceNumber ?? `#${id}`

  const columns: Column<Payment>[] = [
    { header: 'Invoice', cell: (p) => <span className="text-dark font-weight-medium">{invoiceNumber(p.invoiceId)}</span> },
    { header: 'Amount', cell: (p) => money(p.amount, p.currency) },
    { header: 'Gateway', cell: (p) => p.gateway ?? '-' },
    { header: 'Transaction', cell: (p) => p.gatewayTransactionId ?? '-' },
    { header: 'Paid', cell: (p) => (p.paidAt ? p.paidAt.slice(0, 10) : '-') },
    {
      header: 'Status',
      cell: (p) => (
        <StatusSelect
          value={p.status}
          options={PAYMENT_STATUSES}
          disabled={status.isPending}
          onChange={(value) => status.mutate({ id: p.id, value })}
        />
      ),
    },
    {
      header: 'Receipt',
      className: 'text-end',
      cell: (p) =>
        p.hasReceipt ? (
          <span className="badge bg-success">Issued</span>
        ) : p.status === 'CONFIRMED' ? (
          <Button size="sm" disabled={issue.isPending} onClick={() => issue.mutate(p.id)}>
            Issue receipt
          </Button>
        ) : (
          '-'
        ),
    },
  ]

  return (
    <>
      <div className="d-flex justify-content-end mb-2">
        <Button onClick={() => setAdding(true)}>Record payment</Button>
      </div>
      {error && <ErrorBox message={errorMessage(error)} />}
      {status.error && <ErrorBox message={errorMessage(status.error)} />}
      {issue.error && <ErrorBox message={errorMessage(issue.error)} />}
      {receipt && <div className="alert alert-success py-2">{receipt}</div>}
      <DataTable columns={columns} rows={data ?? []} loading={isLoading} empty="No payments yet." />
      {adding && (
        <PaymentModal
          invoices={invoices.data ?? []}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false)
            refresh()
          }}
        />
      )}
    </>
  )
}

type Tab = 'invoices' | 'payments'

export default function BillingPage() {
  const [tab, setTab] = useState<Tab>('invoices')
  return (
    <>
      <PageHeader title="Billing" trail={['Client account']} />
      <Card title="Invoices and payments">
        <Tabs
          tabs={[
            { key: 'invoices', label: 'Invoices' },
            { key: 'payments', label: 'Payments' },
          ]}
          active={tab}
          onChange={setTab}
        />
        {tab === 'invoices' ? <InvoicesTab /> : <PaymentsTab />}
      </Card>
    </>
  )
}
