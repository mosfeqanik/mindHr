import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { crud } from './crud'

type Crud<T, Req> = ReturnType<typeof crud<T, Req>>

/** List query + delete mutation for a crud() resource; `refresh` re-fetches every query with the same key prefix. */
export function useResource<T, Req>(key: string, resource: Crud<T, Req>, params?: Record<string, unknown>) {
  const qc = useQueryClient()
  const query = useQuery({ queryKey: [key, params ?? null], queryFn: () => resource.list(params) })
  const refresh = () => qc.invalidateQueries({ queryKey: [key] })
  const remove = useMutation({ mutationFn: (id: number) => resource.remove(id), onSuccess: refresh })
  return { rows: query.data ?? [], isLoading: query.isLoading, error: query.error, remove, refresh }
}
