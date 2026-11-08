import { crud } from './crud'
import type { Shift, ShiftAssignment, ShiftAssignmentRequest, ShiftRequest } from './types'

export const shiftsApi = crud<Shift, ShiftRequest>('/api/shifts')
export const shiftAssignmentsApi = crud<ShiftAssignment, ShiftAssignmentRequest>('/api/shift-assignments')
