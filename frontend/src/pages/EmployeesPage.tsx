import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { employeesApi } from '../api/employees'
import { errorMessage } from '../api/client'
import type { Employee } from '../api/types'
import { Can } from '../auth/Can'
import { Button, ErrorBox, PageHeader } from '../components/ui'
import EmployeeForm from './EmployeeForm'

const COLUMNS = ['Code', 'Name', 'Email', 'Position', 'Type', 'Status', '']

export default function EmployeesPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<Employee | 'new' | null>(null)
  const [search, setSearch] = useState('')

  const { data, isLoading, error } = useQuery({ queryKey: ['employees'], queryFn: () => employeesApi.list() })

  const remove = useMutation({
    mutationFn: (id: number) => employeesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['employees'] }),
  })

  const rows = (data ?? []).filter((e) =>
    `${e.fullName} ${e.employeeCode} ${e.email ?? ''} ${e.position ?? ''}`.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <>
      <PageHeader
        title="Employees"
        trail={['People']}
        actions={
          <Can permission="EMPLOYEE_CREATE">
            <Button onClick={() => setEditing('new')}>Add employee</Button>
          </Can>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
              <h4 className="card-title mb-0">All employees</h4>
              <input
                type="search"
                className="form-control w-auto mh-search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search name, code, email, position..."
              />
            </div>

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
                        No employees found.
                      </td>
                    </tr>
                  )}
                  {rows.map((e) => (
                    <tr key={e.id}>
                      <td data-label={COLUMNS[0]}>{e.employeeCode}</td>
                      <td data-label={COLUMNS[1]} className="font-weight-medium text-dark">{e.fullName}</td>
                      <td data-label={COLUMNS[2]}>{e.email ?? '-'}</td>
                      <td data-label={COLUMNS[3]}>{e.position ?? '-'}</td>
                      <td data-label={COLUMNS[4]}>{e.employmentType?.replace('_', ' ') ?? '-'}</td>
                      <td data-label={COLUMNS[5]}>
                        <span className={`badge ${e.active ? 'bg-success' : 'bg-secondary'}`}>
                          {e.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td data-label={COLUMNS[6]} className="text-end text-nowrap">
                        <Can permission="EMPLOYEE_UPDATE">
                          <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(e)}>
                            Edit
                          </Button>
                        </Can>
                        <Can permission="EMPLOYEE_DELETE">
                          <Button
                            variant="danger"
                            size="sm"
                            disabled={remove.isPending}
                            onClick={() => confirm(`Delete ${e.fullName}?`) && remove.mutate(e.id)}
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
        <EmployeeForm
          employee={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            qc.invalidateQueries({ queryKey: ['employees'] })
          }}
        />
      )}
    </>
  )
}
