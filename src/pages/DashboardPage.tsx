import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { activityApi } from '../api/activity'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorBox, PageHeader } from '../components/ui'
import { activityBadge, canViewActivity, describeActivity, formatDateTime } from '../lib/activity'
import { label } from '../lib/format'

function RecentActivity() {
  const { isPlatform, orgId } = useAuth()
  const logs = useQuery({ queryKey: ['activity', isPlatform ? orgId : 'own', 'recent'], queryFn: () => activityApi.list(8) })
  return (
    <div className="card">
      <div className="card-body">
        <div className="d-flex align-items-center mb-3">
          <h4 className="card-title mb-0">Recent activity</h4>
          <Link to="/activity" className="ms-auto small">
            View all
          </Link>
        </div>
        {logs.error && <ErrorBox message={errorMessage(logs.error)} />}
        {logs.isLoading && <p className="text-muted mb-0">Loading...</p>}
        {logs.data?.length === 0 && <p className="text-muted mb-0">No activity yet.</p>}
        <ul className="list-unstyled mb-0">
          {logs.data?.map((a) => (
            <li key={a.id} className="d-flex align-items-start py-2 border-bottom">
              <span className={`badge ${activityBadge(a.action)} me-2 mt-1`}>{label(a.action)}</span>
              <div className="flex-grow-1">
                <div>{describeActivity(a)}</div>
                <small className="text-muted">
                  {a.actorName ?? 'Unknown'} &middot; {formatDateTime(a.createdAt)}
                </small>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { session } = useAuth()
  return (
    <>
      <PageHeader title="Dashboard" />
      <div className="container-fluid">
        <div className="row">
          <div className="col-md-6 col-lg-4">
            <div className="card">
              <div className="card-body">
                <h4 className="card-title">Welcome, {session?.fullName}</h4>
                <h6 className="card-subtitle">Signed in as {session?.userType.replace('_', ' ')}</h6>
                <p className="mb-0">
                  Data scope <strong>{session?.dataScope}</strong> &middot; {session?.authorities.length} permissions
                </p>
              </div>
            </div>
          </div>
          {canViewActivity(session?.userType, session?.dataScope) && (
            <div className="col-md-6 col-lg-8">
              <RecentActivity />
            </div>
          )}
        </div>
      </div>
    </>
  )
}
