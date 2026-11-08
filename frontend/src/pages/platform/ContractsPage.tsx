import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage } from '../../api/client'
import { contractsApi } from '../../api/platform'
import { CONTRACT_STATUSES, type Contract, type ContractStatus } from '../../api/types'
import { Button, DataTable, ErrorBox, Field, Input, Modal, ModalFooter, PageHeader, type Column } from '../../components/ui'
import { formatMoney, label, numOrNull, orNull } from '../../lib/format'
import { Badge, Card, HistoryModal, StatusModal } from './shared'

const STATUS_BADGE: Record<ContractStatus, string> = {
  PENDING: 'bg-warning text-dark',
  ACTIVE: 'bg-success',
  EXPIRED: 'bg-secondary',
  TERMINATED: 'bg-danger',
}

const schema = z
  .object({
    title: z.string().min(1, 'Title is required').max(200),
    counterpartyName: z.string().max(150),
    startDate: z.string(),
    endDate: z.string(),
    value: z.string().regex(/^(\d+(\.\d{1,2})?)?$/, 'Enter an amount, e.g. 1200'),
    currency: z.union([z.literal(''), z.string().length(3, '3-letter code')]),
    terms: z.string(),
  })
  .refine((v) => !v.startDate || !v.endDate || v.endDate >= v.startDate, {
    path: ['endDate'],
    message: 'End date must not be before start date',
  })
type Form = z.infer<typeof schema>

function ContractForm({ item, onClose, onSaved }: { item: Contract | null; onClose: () => void; onSaved: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: item?.title ?? '',
      counterpartyName: item?.counterpartyName ?? '',
      startDate: item?.startDate ?? '',
      endDate: item?.endDate ?? '',
      value: item?.value?.toString() ?? '',
      currency: item?.currency ?? 'USD',
      terms: item?.terms ?? '',
    },
  })

  const onSubmit = async (v: Form) => {
    setError(null)
    const body = {
      title: v.title.trim(),
      counterpartyName: orNull(v.counterpartyName),
      startDate: orNull(v.startDate),
      endDate: orNull(v.endDate),
      value: numOrNull(v.value),
      currency: orNull(v.currency.toUpperCase()),
      terms: orNull(v.terms),
    }
    try {
      if (item) await contractsApi.update(item.id, body)
      else await contractsApi.create(body)
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Modal title={item ? 'Edit contract' : 'Add contract'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="modal-body">
          {error && <ErrorBox message={error} />}
          <div className="row">
            <div className="col-md-7">
              <Field label="Title" error={errors.title?.message}>
                <Input {...register('title')} />
              </Field>
            </div>
            <div className="col-md-5">
              <Field label="Counterparty" error={errors.counterpartyName?.message}>
                <Input {...register('counterpartyName')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Start date">
                <Input type="date" {...register('startDate')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="End date" error={errors.endDate?.message}>
                <Input type="date" {...register('endDate')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Value" error={errors.value?.message}>
                <Input inputMode="decimal" {...register('value')} />
              </Field>
            </div>
            <div className="col-md-3">
              <Field label="Currency" error={errors.currency?.message}>
                <Input maxLength={3} {...register('currency')} />
              </Field>
            </div>
          </div>
          <Field label="Terms">
            <textarea className="form-control" rows={4} {...register('terms')} />
          </Field>
        </div>
        <ModalFooter saving={isSubmitting} onClose={onClose} />
      </form>
    </Modal>
  )
}

function ContractHistoryModal({ item, onClose }: { item: Contract; onClose: () => void }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['contract-history', item.id],
    queryFn: () => contractsApi.history(item.id),
  })
  const rows = (data ?? []).map((h) => ({ ...h, change: `${label(h.fromStatus)} → ${label(h.toStatus)}` }))
  return <HistoryModal title={`History of ${item.title}`} rows={rows} loading={isLoading} error={error} onClose={onClose} />
}

type Action = { kind: 'edit' | 'status' | 'history'; item: Contract } | { kind: 'new' }

export default function ContractsPage() {
  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({ queryKey: ['contracts'], queryFn: () => contractsApi.list() })
  const refresh = () => qc.invalidateQueries({ queryKey: ['contracts'] })
  const remove = useMutation({ mutationFn: (id: number) => contractsApi.remove(id), onSuccess: refresh })
  const [action, setAction] = useState<Action | null>(null)
  const done = () => {
    setAction(null)
    refresh()
  }

  const columns: Column<Contract>[] = [
    { header: 'Title', cell: (c) => <span className="text-dark font-weight-medium">{c.title}</span> },
    { header: 'Counterparty', cell: (c) => c.counterpartyName ?? '-' },
    { header: 'Status', cell: (c) => <Badge value={c.status} classes={STATUS_BADGE} /> },
    { header: 'Start', cell: (c) => c.startDate ?? '-' },
    { header: 'End', cell: (c) => c.endDate ?? '-' },
    { header: 'Value', cell: (c) => (c.value == null ? '-' : `${formatMoney(c.value)} ${c.currency ?? ''}`) },
    {
      header: '',
      className: 'text-end text-nowrap',
      cell: (c) => (
        <>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'status', item: c })}>
            Status
          </Button>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'edit', item: c })}>
            Edit
          </Button>
          <Button variant="secondary" size="sm" className="me-2" onClick={() => setAction({ kind: 'history', item: c })}>
            History
          </Button>
          <Button
            variant="danger"
            size="sm"
            disabled={remove.isPending}
            onClick={() => confirm(`Delete contract ${c.title}?`) && remove.mutate(c.id)}
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
        title="Contracts"
        trail={['Client account']}
        actions={<Button onClick={() => setAction({ kind: 'new' })}>Add contract</Button>}
      />
      <Card title="Contracts">
        {error && <ErrorBox message={errorMessage(error)} />}
        {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
        <DataTable columns={columns} rows={data ?? []} loading={isLoading} empty="No contracts yet." />
      </Card>
      {action?.kind === 'new' && <ContractForm item={null} onClose={() => setAction(null)} onSaved={done} />}
      {action?.kind === 'edit' && <ContractForm item={action.item} onClose={() => setAction(null)} onSaved={done} />}
      {action?.kind === 'status' && (
        <StatusModal
          title="Change contract status"
          current={action.item.status}
          options={CONTRACT_STATUSES}
          onClose={() => setAction(null)}
          onSave={(status, note) => contractsApi.changeStatus(action.item.id, status, note)}
          onDone={done}
        />
      )}
      {action?.kind === 'history' && <ContractHistoryModal item={action.item} onClose={() => setAction(null)} />}
    </>
  )
}
