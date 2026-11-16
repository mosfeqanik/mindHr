import { crud } from './crud'
import type { Attendance, AttendanceRequest } from './types'

export const attendanceApi = crud<Attendance, AttendanceRequest>('/api/attendance')
