import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { branchesApi } from '../api/branches'
import { errorMessage } from '../api/client'
import { useEmployeeLookup } from '../api/lookups'
import type { Branch } from '../api/types'
import { Can } from '../auth/Can'
import { Button, ErrorBox, PageHeader } from '../components/ui'
import BranchForm from './BranchForm'

const COLUMNS = ['Code', 'Name', 'Address', 'Phone', 'Manager', 'Geofence', 'Status', '']

export default function BranchesPage() {
  const qc = useQueryClient()
  const { nameOf } = useEmployeeLookup()
  const [editing, setEditing] = useState<Branch | 'new' | null>(null)

  const { data, isLoading, error } = useQuery({ queryKey: ['branches'], queryFn: () => branchesApi.list() })
  const remove = useMutation({
    mutationFn: (id: number) => branchesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['branches'] }),
  })
  const rows = data ?? []

  return (
    <>
      <PageHeader
        title="Branches"
        trail={['Organization']}
        actions={
          <Can permission="BRANCH_CREATE">
            <Button onClick={() => setEditing('new')}>Add branch</Button>
          </Can>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <h4 className="card-title">All branches</h4>
            {error && <ErrorBox message={errorMessage(error)} />}
            {remove.error && <ErrorBox message={errorMessage(remove.error)} />}
            <div className="table-responsive">
              <table className="table table-hover align-middle mh-stack">
                <thead>
                  <tr>
                    {COLUMNS.map((h, i) => (
                      <th key={i} scope="col">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {isLoading && (
                    <tr>
                      <td colSpan={COLUMNS.length} className="text-center text-muted">
                        Loading...
                      </td>
                    </tr>
                  )}
                  {!isLoading && rows.length === 0 && (
                    <tr>
                      <td colSpan={COLUMNS.length} className="text-center text-muted">
                        No branches yet.
                      </td>
                    </tr>
                  )}
                  {rows.map((b) => (
                    <tr key={b.id}>
                      <td data-label={COLUMNS[0]}>{b.code ?? '-'}</td>
                      <td data-label={COLUMNS[1]} className="font-weight-medium text-dark">{b.name}</td>
                      <td data-label={COLUMNS[2]}>{b.address ?? '-'}</td>
                      <td data-label={COLUMNS[3]}>{b.phone ?? '-'}</td>
                      <td data-label={COLUMNS[4]}>{nameOf(b.managerId)}</td>
                      <td data-label={COLUMNS[5]}>
                        {b.latitude != null && b.longitude != null
                          ? `${b.latitude}, ${b.longitude} (${b.geofenceRadiusM ?? '-'} m)`
                          : '-'}
                      </td>
                      <td data-label={COLUMNS[6]}>
                        <span className={`badge ${b.active ? 'bg-success' : 'bg-secondary'}`}>
                          {b.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td data-label={COLUMNS[7]} className="text-end text-nowrap">
                        <Can permission="BRANCH_UPDATE">
                          <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(b)}>
                            Edit
                          </Button>
                        </Can>
                        <Can permission="BRANCH_DELETE">
                          <Button
                            variant="danger"
                            size="sm"
                            disabled={remove.isPending}
                            onClick={() => confirm(`Delete ${b.name}?`) && remove.mutate(b.id)}
                          >
                            Delete
                          </Button>
                        </Can>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {editing && (
        <BranchForm
          branch={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            qc.invalidateQueries({ queryKey: ['branches'] })
          }}
        />
      )}
    </>
  )
}
