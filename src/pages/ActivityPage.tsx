import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { activityApi } from '../api/activity'
import { errorMessage } from '../api/client'
import { organizationsApi } from '../api/platform'
import type { ActivityLog } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { DataTable, ErrorBox, PageHeader, Select, type Column } from '../components/ui'
import { activityBadge, describeActivity, formatDateTime } from '../lib/activity'
import { label } from '../lib/format'

const LIMITS = [100, 250, 500]

/** Audit trail: every create / update / delete / approve ... done through the API, newest first. */
export default function ActivityPage() {
  const { isPlatform, orgId } = useAuth()
  const [limit, setLimit] = useState(LIMITS[0])
  const logs = useQuery({ queryKey: ['activity', isPlatform ? orgId : 'own', limit], queryFn: () => activityApi.list(limit) })
  const orgs = useQuery({ queryKey: ['organizations'], queryFn: () => organizationsApi.list(), enabled: isPlatform })
  const orgName = (id: number | null) =>
    id == null ? 'Platform' : (orgs.data?.find((o) => o.id === id)?.name ?? `#${id}`)

  const columns: Column<ActivityLog>[] = [
    { header: 'When', cell: (a) => <span className="text-nowrap">{formatDateTime(a.createdAt)}</span>, value: (a) => a.createdAt },
    { header: 'Who', cell: (a) => a.actorName ?? '-', value: (a) => a.actorName },
    {
      header: 'Action',
      cell: (a) => <span className={`badge ${activityBadge(a.action)}`}>{label(a.action)}</span>,
      value: (a) => a.action,
    },
    { header: 'What', cell: (a) => describeActivity(a), value: (a) => describeActivity(a) },
    ...(isPlatform && orgId == null
      ? [{ header: 'Organization', cell: (a: ActivityLog) => orgName(a.organizationId), value: (a: ActivityLog) => orgName(a.organizationId) }]
      : []),
    { header: 'IP', cell: (a) => <small className="text-muted">{a.ipAddress ?? '-'}</small>, value: (a) => a.ipAddress },
  ]

  return (
    <>
      <PageHeader
        title="Activity"
        actions={
          <Select className="form-select-sm w-auto" value={limit} onChange={(e) => setLimit(Number(e.target.value))}>
            {LIMITS.map((l) => (
              <option key={l} value={l}>
                Last {l}
              </option>
            ))}
          </Select>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <h6 className="card-subtitle mb-3">
              {isPlatform && orgId == null
                ? 'All organizations. Pick an organization in the top bar to see only its activity.'
                : 'Changes made in this organization.'}
            </h6>
            {logs.error && <ErrorBox message={errorMessage(logs.error)} />}
            <DataTable
              datatable
              columns={columns}
              rows={logs.data ?? []}
              loading={logs.isLoading}
              empty="No activity yet. Creating, updating or approving anything will show up here."
            />
          </div>
        </div>
      </div>
    </>
  )
}
