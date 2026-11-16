import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { departmentsApi } from '../api/departments'
import { errorMessage } from '../api/client'
import { useBranchLookup, useEmployeeLookup } from '../api/lookups'
import type { Department } from '../api/types'
import { Can } from '../auth/Can'
import { Button, ErrorBox, PageHeader } from '../components/ui'
import DepartmentForm from './DepartmentForm'

const COLUMNS = ['Name', 'Branch', 'Head', 'Status', '']

export default function DepartmentsPage() {
  const qc = useQueryClient()
  const branches = useBranchLookup()
  const employees = useEmployeeLookup()
  const [editing, setEditing] = useState<Department | 'new' | null>(null)

  const { data, isLoading, error } = useQuery({ queryKey: ['departments'], queryFn: () => departmentsApi.list() })
  const remove = useMutation({
    mutationFn: (id: number) => departmentsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['departments'] }),
  })
  const rows = data ?? []

  return (
    <>
      <PageHeader
        title="Departments"
        trail={['Organization']}
        actions={
          <Can permission="DEPARTMENT_CREATE">
            <Button onClick={() => setEditing('new')}>Add department</Button>
          </Can>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <h4 className="card-title">All departments</h4>
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
                        No departments yet.
                      </td>
                    </tr>
                  )}
                  {rows.map((d) => (
                    <tr key={d.id}>
                      <td data-label={COLUMNS[0]} className="font-weight-medium text-dark">{d.name}</td>
                      <td data-label={COLUMNS[1]}>{branches.nameOf(d.branchId)}</td>
                      <td data-label={COLUMNS[2]}>{employees.nameOf(d.headId)}</td>
                      <td data-label={COLUMNS[3]}>
                        <span className={`badge ${d.active ? 'bg-success' : 'bg-secondary'}`}>
                          {d.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td data-label={COLUMNS[4]} className="text-end text-nowrap">
                        <Can permission="DEPARTMENT_UPDATE">
                          <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(d)}>
                            Edit
                          </Button>
                        </Can>
                        <Can permission="DEPARTMENT_DELETE">
                          <Button
                            variant="danger"
                            size="sm"
                            disabled={remove.isPending}
                            onClick={() => confirm(`Delete ${d.name}?`) && remove.mutate(d.id)}
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
        <DepartmentForm
          department={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            qc.invalidateQueries({ queryKey: ['departments'] })
          }}
        />
      )}
    </>
  )
}
