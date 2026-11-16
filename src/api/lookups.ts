import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { branchesApi } from './branches'
import { assetsApi } from './assets'
import { employeesApi } from './employees'
import { leaveTypesApi } from './leave'
import { shiftsApi } from './shifts'

/** Employee id -> name for dropdowns and tables. Empty when the user lacks EMPLOYEE_VIEW. */
export function useEmployeeLookup() {
  const { can } = useAuth()
  const { data } = useQuery({
    queryKey: ['employees'],
    queryFn: () => employeesApi.list(),
    enabled: can('EMPLOYEE_VIEW'),
  })
  const list = data ?? []
  const nameOf = (id: number | null | undefined) =>
    id == null ? '-' : (list.find((e) => e.id === id)?.fullName ?? `#${id}`)
  return { employees: list, nameOf }
}

/** Leave type id -> name. Empty when the user lacks LEAVE_VIEW. */
export function useLeaveTypeLookup() {
  const { can } = useAuth()
  const { data } = useQuery({
    queryKey: ['leave-types', null],
    queryFn: () => leaveTypesApi.list(),
    enabled: can('LEAVE_VIEW'),
  })
  const list = data ?? []
  const nameOf = (id: number | null | undefined) =>
    id == null ? '-' : (list.find((t) => t.id === id)?.name ?? `#${id}`)
  return { leaveTypes: list, nameOf }
}

/** Asset id -> "name (tag)". Empty when the user lacks ASSET_VIEW. */
export function useAssetLookup() {
  const { can } = useAuth()
  const { data } = useQuery({
    queryKey: ['assets', null],
    queryFn: () => assetsApi.list(),
    enabled: can('ASSET_VIEW'),
  })
  const list = data ?? []
  const nameOf = (id: number | null | undefined) => {
    if (id == null) return '-'
    const a = list.find((x) => x.id === id)
    return a ? `${a.name} (${a.assetTag})` : `#${id}`
  }
  return { assets: list, nameOf }
}

/** Shift id -> name. Empty when the user lacks SHIFT_VIEW. */
export function useShiftLookup() {
  const { can } = useAuth()
  const { data } = useQuery({
    queryKey: ['shifts', null],
    queryFn: () => shiftsApi.list(),
    enabled: can('SHIFT_VIEW'),
  })
  const list = data ?? []
  const nameOf = (id: number | null | undefined) =>
    id == null ? '-' : (list.find((s) => s.id === id)?.name ?? `#${id}`)
  return { shifts: list, nameOf }
}

/** Branch id -> name. Empty when the user lacks BRANCH_VIEW. */
export function useBranchLookup() {
  const { can } = useAuth()
  const { data } = useQuery({
    queryKey: ['branches'],
    queryFn: () => branchesApi.list(),
    enabled: can('BRANCH_VIEW'),
  })
  const list = data ?? []
  const nameOf = (id: number | null | undefined) =>
    id == null ? '-' : (list.find((b) => b.id === id)?.name ?? `#${id}`)
  return { branches: list, nameOf }
}
