import { crud } from './crud'
import type { Branch, BranchRequest } from './types'

export const branchesApi = crud<Branch, BranchRequest>('/api/branches')
