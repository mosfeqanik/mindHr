import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Edit2, Eye, Trash2 } from 'react-feather'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { subscriptionPlansApi } from '../../api/platform'
import { BILLING_CYCLES, PLAN_FEATURES, type SubscriptionPlan } from '../../api/types'
import { useResource } from '../../api/useResource'
import { useAuth } from '../../auth/AuthContext'
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
  Select,
  type Column,
} from '../../components/ui'
import { formatMoney, label, numOrNull, orNull } from '../../lib/format'
import { AddButton, Card } from './shared'

const count = z.string().regex(/^\d*$/, 'Whole number')

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  description: z.string().max(500),
  price: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a price, e.g. 49.99'),
  currency: z.union([z.literal(''), z.string().length(3, '3-letter code')]),
  billingCycle: z.enum(BILLING_CYCLES),
  maxEmployees: count,
  maxBranches: count,
  trialDays: count,
  features: z.array(z.enum(PLAN_FEATURES)),
  active: z.boolean(),
})
type Form = z.infer<typeof schema>

function PlanForm({ item, onClose, onSaved }: { item: SubscriptionPlan | null; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: item?.name ?? '',
      description: item?.description ?? '',
      price: item?.price.toString() ?? '',
      currency: item?.currency ?? 'USD',
      billingCycle: item?.billingCycle ?? 'MONTHLY',
      maxEmployees: item?.maxEmployees?.toString() ?? '',
      maxBranches: item?.maxBranches?.toString() ?? '',
      trialDays: item?.trialDays.toString() ?? '14',
      features: item?.features ?? [],
      active: item?.active ?? true,
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = {
      name: v.name.trim(),
      description: orNull(v.description),
      price: Number(v.price),
      currency: orNull(v.currency.toUpperCase()),
      billingCycle: v.billingCycle,
      maxEmployees: numOrNull(v.maxEmployees),
      maxBranches: numOrNull(v.maxBranches),
      trialDays: numOrNull(v.trialDays) ?? 0,
      features: v.features,
      active: v.active,
    }
    try {
      if (item) await subscriptionPlansApi.update(item.id, body)
      else await subscriptionPlansApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit plan' : 'Add plan'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-6">
              <Field label="Name" error={errors.name?.message}>
                <Input {...register('name')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Price" error={errors.price?.message}>
                <Input inputMode="decimal" {...register('price')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Currency" error={errors.currency?.message}>
                <Input maxLength={3} {...register('currency')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Billing cycle">
                <Select {...register('billingCycle')}>
                  {BILLING_CYCLES.map((c) => (
                    <option key={c} value={c}>
                      {label(c)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Max employees" error={errors.maxEmployees?.message}>
                <Input inputMode="numeric" placeholder="No limit" {...register('maxEmployees')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Max branches" error={errors.maxBranches?.message}>
                <Input inputMode="numeric" placeholder="No limit" {...register('maxBranches')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Trial days" error={errors.trialDays?.message}>
                <Input inputMode="numeric" {...register('trialDays')} />
              </Field>
            </div>
          </div>
          <Field label="Description" error={errors.description?.message}>
            <Input {...register('description')} />
          </Field>
          <Field label="Features">
            <div className="row">
              {PLAN_FEATURES.map((f) => (
                <div key={f} className="col-md-4">
                  <label className="form-check">
                    <input type="checkbox" className="form-check-input" value={f} {...register('features')} />
                    <span className="form-check-label">{label(f)}</span>
                  </label>
                </div>
              ))}
            </div>
          </Field>
          <label className="form-check">
            <input type="checkbox" className="form-check-input" {...register('active')} />
            <span className="form-check-label">Active (can be sold)</span>
          </label>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

const priceText = (p: SubscriptionPlan) =>
  `${formatMoney(p.price)} ${p.currency ?? ''} / ${p.billingCycle === 'MONTHLY' ? 'month' : 'year'}`

/** Read-only details of a plan, with a shortcut to the edit form. */
function PlanDetails({
  plan,
  canEdit,
  onClose,
  onEdit,
}: {
  plan: SubscriptionPlan
  canEdit: boolean
  onClose: () => void
  onEdit: () => void
}) {
  const rows: [string, React.ReactNode][] = [
    ['Name', plan.name],
    ['Description', plan.description || '-'],
    ['Price', priceText(plan)],
    ['Billing cycle', label(plan.billingCycle)],
    ['Max employees', plan.maxEmployees ?? 'No limit'],
    ['Max branches', plan.maxBranches ?? 'No limit'],
    ['Trial', `${plan.trialDays} days`],
    ['Features', plan.features.map(label).join(', ') || '-'],
    ['Status', <ActiveBadge active={plan.active} />],
  ]
  return (
    <Modal title={plan.name} onClose={onClose}>
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
        {canEdit && (
          <Button onClick={onEdit}>
            <Edit2 size={15} className="me-1" />
            Edit
          </Button>
        )}
      </div>
    </Modal>
  )
}

export default function SubscriptionPlansPage() {
  const { session } = useAuth()
  const canEdit = session?.userType === 'SUPER_ADMIN'
  const { rows, isLoading, error, remove, refresh } = useResource('subscription-plans', subscriptionPlansApi)
  const [editing, setEditing] = useState<SubscriptionPlan | 'new' | null>(null)
  const [viewing, setViewing] = useState<SubscriptionPlan | null>(null)

  const columns: Column<SubscriptionPlan>[] = [
    { header: 'Name', cell: (p) => <span className="text-dark font-weight-medium">{p.name}</span>, value: (p) => p.name },
    {
      header: 'Price',
      cell: priceText,
      value: (p) => p.price,
    },
    { header: 'Employees', cell: (p) => p.maxEmployees ?? 'No limit', value: (p) => p.maxEmployees ?? Infinity },
    { header: 'Branches', cell: (p) => p.maxBranches ?? 'No limit', value: (p) => p.maxBranches ?? Infinity },
    { header: 'Trial', cell: (p) => `${p.trialDays} days`, value: (p) => p.trialDays },
    {
      header: 'Features',
      cell: (p) => <small>{p.features.map(label).join(', ') || '-'}</small>,
      value: (p) => p.features.map(label).join(', '),
    },
    { header: 'Status', cell: (p) => <ActiveBadge active={p.active} />, value: (p) => (p.active ? 'Active' : 'Inactive') },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (p) => (
        <>
          <IconButton icon={<Eye size={15} />} label="View" className="me-1" onClick={() => setViewing(p)} />
          {canEdit && (
            <>
              <IconButton icon={<Edit2 size={15} />} label="Edit" className="me-1" onClick={() => setEditing(p)} />
              <IconButton
                icon={<Trash2 size={15} />}
                label="Delete"
                variant="danger"
                disabled={remove.isPending}
                onClick={() => confirm(`Delete plan ${p.name}?`) && remove.mutate(p.id)}
              />
            </>
          )}
        </>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="Subscription Plans" trail={['Platform']} />
      <Card title="Plans" actions={canEdit && <AddButton onClick={() => setEditing('new')}>Add plan</AddButton>}>
        {error && <ErrorBox message={errorMessage(error)} />}
        {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
        <DataTable datatable columns={columns} rows={rows} loading={isLoading} empty="No plans yet." />
      </Card>
      {viewing && (
        <PlanDetails
          plan={viewing}
          canEdit={canEdit}
          onClose={() => setViewing(null)}
          onEdit={() => {
            // Swap the details dialog for the edit form of the same plan.
            setEditing(viewing)
            setViewing(null)
          }}
        />
      )}
      {editing && (
        <PlanForm
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
