import { api } from './client'
import type { ActivityLog } from './types'

// Platform users with an organization picked get it added as a filter (orgScope.ts); without one they see all.
export const activityApi = {
  list: (limit?: number) => api.get<ActivityLog[]>('/api/activity-logs', { params: { limit } }).then((r) => r.data),
}
