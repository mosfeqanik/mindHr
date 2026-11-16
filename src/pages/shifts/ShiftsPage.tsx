import { useState } from 'react'
import { PageHeader, Tabs } from '../../components/ui'
import RosterTab from './RosterTab'
import ShiftsTab from './ShiftsTab'

type Tab = 'roster' | 'shifts'

export default function ShiftsPage() {
  const [tab, setTab] = useState<Tab>('roster')
  const tabs: { key: Tab; label: string }[] = [
    { key: 'roster', label: 'Roster' },
    { key: 'shifts', label: 'Shifts' },
  ]

  return (
    <>
      <PageHeader title="Shifts" trail={['Time']} />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <Tabs tabs={tabs} active={tab} onChange={setTab} />
            {tab === 'roster' && <RosterTab />}
            {tab === 'shifts' && <ShiftsTab />}
          </div>
        </div>
      </div>
    </>
  )
}
