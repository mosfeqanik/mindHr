package com.mindhr.mobile.data

import kotlinx.serialization.Serializable

// Mirrors backend DTO records. Dates are ISO strings ("2026-11-03", "2026-11-03T09:05:12");
// BigDecimal amounts arrive as JSON numbers.

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 0,
    val fullName: String = "",
    val userType: String = "",
)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class Employee(
    val id: Long,
    val organizationId: Long? = null,
    val userId: Long? = null,
    val employeeCode: String = "",
    val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val position: String? = null,
    val nationalIdSerial: String? = null,
    val photoUrl: String? = null,
    val address: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelation: String? = null,
    val branchId: Long? = null,
    val departmentId: Long? = null,
    val managerId: Long? = null,
    val employmentType: String? = null,
    val joinedAt: String? = null,
    val active: Boolean = true,
)

@Serializable
data class Branch(
    val id: Long,
    val name: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceRadiusM: Int? = null,
)

@Serializable
data class Department(val id: Long, val name: String = "")

@Serializable
data class Attendance(
    val id: Long,
    val employeeId: Long? = null,
    val workDate: String,
    val checkInAt: String? = null,
    val checkOutAt: String? = null,
    val method: String? = null,
    val status: String? = null,
    val lateMinutes: Int = 0,
    val workedMinutes: Int = 0,
    val note: String? = null,
)

@Serializable
data class CheckInRequest(
    val method: String = "MOBILE_GPS",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val note: String? = null,
)

@Serializable
data class LeaveType(
    val id: Long,
    val name: String = "",
    val annualDays: Int = 0,
    val paid: Boolean = true,
    val active: Boolean = true,
)

@Serializable
data class LeaveBalance(
    val id: Long,
    val employeeId: Long? = null,
    val leaveTypeId: Long? = null,
    val year: Int = 0,
    val entitledDays: Double = 0.0,
    val usedDays: Double = 0.0,
)

@Serializable
data class LeaveRequest(
    val id: Long,
    val employeeId: Long? = null,
    val leaveTypeId: Long? = null,
    val fromDate: String,
    val toDate: String,
    val days: Double = 0.0,
    val reason: String? = null,
    val status: String = "PENDING",
    val decidedAt: String? = null,
    val decisionNote: String? = null,
)

@Serializable
data class LeaveApplyRequest(
    val leaveTypeId: Long,
    val fromDate: String,
    val toDate: String,
    val reason: String? = null,
)

@Serializable
data class DecisionRequest(val note: String? = null)

@Serializable
data class Payslip(
    val id: Long,
    val payrollRunId: Long? = null,
    val employeeId: Long? = null,
    val basicSalary: Double = 0.0,
    val allowances: Double = 0.0,
    val overtime: Double = 0.0,
    val bonus: Double = 0.0,
    val commission: Double = 0.0,
    val grossSalary: Double = 0.0,
    val lateDeduction: Double = 0.0,
    val absentDeduction: Double = 0.0,
    val loanDeduction: Double = 0.0,
    val advanceDeduction: Double = 0.0,
    val netSalary: Double = 0.0,
    val daysPresent: Int = 0,
    val daysAbsent: Int = 0,
    val daysLate: Int = 0,
    val status: String? = null,
)

@Serializable
data class WorkRequest(
    val id: Long,
    val employeeId: Long? = null,
    val type: String,
    val title: String = "",
    val details: String? = null,
    val amount: Double? = null,
    val requestDate: String? = null,
    val status: String = "PENDING",
    val decidedAt: String? = null,
    val decisionNote: String? = null,
)

@Serializable
data class WorkRequestCreate(
    val type: String,
    val title: String,
    val details: String? = null,
    val amount: Double? = null,
    val requestDate: String? = null,
)

@Serializable
data class AppNotification(
    val id: Long,
    val channel: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val sentAt: String? = null,
    val read: Boolean = false,
)

@Serializable
data class ErrorBody(val message: String? = null, val error: String? = null)
