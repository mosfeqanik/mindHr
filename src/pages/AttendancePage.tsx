import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { attendanceApi } from '../api/attendance'
import { errorMessage } from '../api/client'
import { useEmployeeLookup } from '../api/lookups'
import type { Attendance, AttendanceStatus } from '../api/types'
import { Can } from '../auth/Can'
import { Button, ErrorBox, PageHeader } from '../components/ui'
import AttendanceForm from './AttendanceForm'

const COLUMNS = ['Date', 'Employee', 'Check in', 'Check out', 'Worked', 'Late', 'Status', 'Method', '']

const STATUS_BADGE: Record<AttendanceStatus, string> = {
  PRESENT: 'bg-success',
  LATE: 'bg-warning text-dark',
  ABSENT: 'bg-danger',
  HALF_DAY: 'bg-info',
  ON_LEAVE: 'bg-secondary',
}

const time = (v: string | null) => (v ? v.slice(11, 16) : '-')
const hm = (minutes: number) => `${Math.floor(minutes / 60)}h ${minutes % 60}m`

export default function AttendancePage() {
  const qc = useQueryClient()
  const { employees, nameOf } = useEmployeeLookup()
  const [editing, setEditing] = useState<Attendance | 'new' | null>(null)
  const [employeeId, setEmployeeId] = useState('')
  const [date, setDate] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['attendance', employeeId],
    queryFn: () => attendanceApi.list(employeeId ? { employeeId: Number(employeeId) } : undefined),
  })
  const remove = useMutation({
    mutationFn: (id: number) => attendanceApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['attendance'] }),
  })

  const rows = (data ?? [])
    .filter((a) => !date || a.workDate === date)
    .sort((a, b) => b.workDate.localeCompare(a.workDate))

  return (
    <>
      <PageHeader
        title="Attendance"
        trail={['Time']}
        actions={
          <Can permission="ATTENDANCE_MANAGE">
            <Button onClick={() => setEditing('new')}>Add record</Button>
          </Can>
        }
      />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
              <h4 className="card-title mb-0">Attendance records</h4>
              <div className="d-flex gap-2">
                {employees.length > 0 && (
                  <select
                    className="form-select w-auto"
                    value={employeeId}
                    onChange={(e) => setEmployeeId(e.target.value)}
                  >
                    <option value="">All employees</option>
                    {employees.map((e) => (
                      <option key={e.id} value={e.id}>
                        {e.fullName}
                      </option>
                    ))}
                  </select>
                )}
                <input
                  type="date"
                  className="form-control w-auto"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </div>
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
                        No attendance records.
                      </td>
                    </tr>
                  )}
                  {rows.map((a) => (
                    <tr key={a.id}>
                      <td data-label={COLUMNS[0]}>{a.workDate}</td>
                      <td data-label={COLUMNS[1]} className="font-weight-medium text-dark">{nameOf(a.employeeId)}</td>
                      <td data-label={COLUMNS[2]}>{time(a.checkInAt)}</td>
                      <td data-label={COLUMNS[3]}>{time(a.checkOutAt)}</td>
                      <td data-label={COLUMNS[4]}>{hm(a.workedMinutes)}</td>
                      <td data-label={COLUMNS[5]}>{a.lateMinutes > 0 ? `${a.lateMinutes} min` : '-'}</td>
                      <td data-label={COLUMNS[6]}>
                        {a.status ? (
                          <span className={`badge ${STATUS_BADGE[a.status]}`}>{a.status.replace('_', ' ')}</span>
                        ) : (
                          '-'
                        )}
                      </td>
                      <td data-label={COLUMNS[7]}>{a.method?.replace('_', ' ') ?? '-'}</td>
                      <td data-label={COLUMNS[8]} className="text-end text-nowrap">
                        <Can permission="ATTENDANCE_MANAGE">
                          <Button variant="secondary" size="sm" className="me-2" onClick={() => setEditing(a)}>
                            Edit
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            disabled={remove.isPending}
                            onClick={() => confirm('Delete this attendance record?') && remove.mutate(a.id)}
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
        <AttendanceForm
          record={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            qc.invalidateQueries({ queryKey: ['attendance'] })
          }}
        />
      )}
    </>
  )
}
