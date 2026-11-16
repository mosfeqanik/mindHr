import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'

/** Renders children only when the signed-in user holds the authority, e.g. EMPLOYEE_CREATE. */
export function Can({ permission, children }: { permission: string; children: ReactNode }) {
  const { can } = useAuth()
  return can(permission) ? <>{children}</> : null
}
