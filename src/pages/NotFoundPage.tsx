import { Link } from 'react-router-dom'
import { PageHeader } from '../components/ui'

export default function NotFoundPage() {
  return (
    <>
      <PageHeader title="Page not found" />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body">
            <p className="text-muted">This page does not exist.</p>
            <Link to="/" className="btn btn-outline-secondary">
              Back to dashboard
            </Link>
          </div>
        </div>
      </div>
    </>
  )
}
