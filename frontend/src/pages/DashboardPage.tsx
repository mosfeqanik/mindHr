import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/ui'

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
        </div>
      </div>
    </>
  )
}
