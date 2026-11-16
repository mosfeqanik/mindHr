import { useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { PageHeader, Tabs } from '../../components/ui'
import AssetsTab from './AssetsTab'
import AssignmentsTab from './AssignmentsTab'
import DocumentsTab from './DocumentsTab'

type Tab = 'assets' | 'assignments' | 'documents'

export default function AssetsPage() {
  const { can } = useAuth()
  const [tab, setTab] = useState<Tab>('assets')
  const tabs: { key: Tab; label: string }[] = [
    { key: 'assets', label: 'Assets' },
    { key: 'assignments', label: 'Assignments' },
    // Documents have their own DOCUMENT_* permissions, separate from ASSET_*.
    ...(can('DOCUMENT_VIEW') ? [{ key: 'documents' as const, label: 'Documents' }] : []),
  ]

  return (
    <>
      <PageHeader title="Assets" trail={['People']} />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <Tabs tabs={tabs} active={tab} onChange={setTab} />
            {tab === 'assets' && <AssetsTab />}
            {tab === 'assignments' && <AssignmentsTab />}
            {tab === 'documents' && <DocumentsTab />}
          </div>
        </div>
      </div>
    </>
  )
}
