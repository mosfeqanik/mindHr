import { useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { PageHeader, Tabs } from '../../components/ui'
import LeaveBalancesTab from './LeaveBalancesTab'
import LeaveRequestsTab from './LeaveRequestsTab'
import LeaveTypesTab from './LeaveTypesTab'

type Tab = 'requests' | 'balances' | 'types'

export default function LeavePage() {
  const { can } = useAuth()
  const [tab, setTab] = useState<Tab>('requests')
  const tabs: { key: Tab; label: string }[] = [
    { key: 'requests', label: 'Requests' },
    { key: 'balances', label: 'Balances' },
    { key: 'types', label: 'Leave types' },
  ]

  return (
    <>
      <PageHeader title="Leave" trail={['Time']} />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <Tabs tabs={tabs} active={tab} onChange={setTab} />
            {!can('LEAVE_VIEW') && <p className="text-muted">You do not have permission to view leave.</p>}
            {tab === 'requests' && <LeaveRequestsTab />}
            {tab === 'balances' && <LeaveBalancesTab />}
            {tab === 'types' && <LeaveTypesTab />}
          </div>
        </div>
      </div>
    </>
  )
}
