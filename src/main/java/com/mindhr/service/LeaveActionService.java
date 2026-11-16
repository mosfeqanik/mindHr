package com.mindhr.service;

import com.mindhr.dto.LeaveApplyRequest;
import com.mindhr.dto.LeaveRequestResponse;
import com.mindhr.entity.Attendance;
import com.mindhr.entity.Employee;
import com.mindhr.entity.LeaveBalance;
import com.mindhr.entity.LeaveRequest;
import com.mindhr.entity.LeaveType;
import com.mindhr.entity.enums.AttendanceMethod;
import com.mindhr.entity.enums.AttendanceStatus;
import com.mindhr.entity.enums.EmployeeEventType;
import com.mindhr.entity.enums.LeaveStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.LeaveRequestMapper;
import com.mindhr.repository.AttendanceRepository;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.LeaveBalanceRepository;
import com.mindhr.repository.LeaveRequestRepository;
import com.mindhr.repository.LeaveTypeRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Leave workflow: the employee applies, their manager (anyone with LEAVE_APPROVE whose data scope
 * covers the employee) approves or rejects. Leave days are calendar days, weekends included, and a
 * request cannot span two calendar years. Unpaid leave types are not limited by a balance.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LeaveActionService {

    private static final List<LeaveStatus> OPEN = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final DataScopeService scope;
    private final EmployeeTimelineService timeline;

    public LeaveRequestResponse apply(LeaveApplyRequest request) {
        Employee employee = currentEmployee();
        Long organizationId = employee.getOrganization().getId();

        if (request.toDate().isBefore(request.fromDate())) {
            throw new IllegalArgumentException("To date must not be before from date");
        }
        if (request.fromDate().getYear() != request.toDate().getYear()) {
            throw new IllegalArgumentException("Leave cannot span two calendar years; apply for each year separately");
        }
        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .filter(t -> t.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + request.leaveTypeId()));
        if (!leaveType.isActive()) {
            throw new IllegalArgumentException("Leave type " + leaveType.getName() + " is not active");
        }

        boolean overlaps = leaveRequestRepository.findByEmployeeIdAndStatusIn(employee.getId(), OPEN).stream()
                .anyMatch(r -> !r.getFromDate().isAfter(request.toDate()) && !r.getToDate().isBefore(request.fromDate()));
        if (overlaps) {
            throw new IllegalArgumentException("You already have leave requested or approved for these dates");
        }

        BigDecimal days = BigDecimal.valueOf(ChronoUnit.DAYS.between(request.fromDate(), request.toDate()) + 1);
        int year = request.fromDate().getYear();
        if (leaveType.isPaid()) {
            LeaveBalance balance = balanceFor(employee, leaveType, year);
            BigDecimal reserved = leaveRequestRepository
                    .findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING).stream()
                    .filter(r -> r.getLeaveType().getId().equals(leaveType.getId()) && r.getFromDate().getYear() == year)
                    .map(LeaveRequest::getDays)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal available = balance.getEntitledDays().subtract(balance.getUsedDays()).subtract(reserved);
            if (days.compareTo(available) > 0) {
                throw new IllegalArgumentException("Insufficient " + leaveType.getName() + " balance: "
                        + available.stripTrailingZeros().toPlainString() + " day(s) available, "
                        + days.toPlainString() + " requested");
            }
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setOrganization(employee.getOrganization());
        leave.setEmployee(employee);
        leave.setLeaveType(leaveType);
        leave.setFromDate(request.fromDate());
        leave.setToDate(request.toDate());
        leave.setDays(days);
        leave.setReason(request.reason());
        leave.setStatus(LeaveStatus.PENDING);
        return LeaveRequestMapper.toResponse(leaveRequestRepository.save(leave));
    }

    public LeaveRequestResponse approve(Long id, String note) {
        LeaveRequest leave = findDecidable(id);
        LeaveType leaveType = leave.getLeaveType();
        Employee employee = leave.getEmployee();

        if (leaveType.isPaid()) {
            LeaveBalance balance = balanceFor(employee, leaveType, leave.getFromDate().getYear());
            BigDecimal remaining = balance.getEntitledDays().subtract(balance.getUsedDays());
            if (leave.getDays().compareTo(remaining) > 0) {
                throw new IllegalArgumentException("Insufficient leave balance: "
                        + remaining.stripTrailingZeros().toPlainString() + " day(s) remaining");
            }
            balance.setUsedDays(balance.getUsedDays().add(leave.getDays()));
        }
        markOnLeave(leave);
        decide(leave, LeaveStatus.APPROVED, note);
        timeline.record(employee, EmployeeEventType.LEAVE_APPROVED, leave.getFromDate(),
                leaveType.getName() + " approved", null,
                leave.getFromDate() + " to " + leave.getToDate(), note);
        return LeaveRequestMapper.toResponse(leave);
    }

    public LeaveRequestResponse reject(Long id, String note) {
        LeaveRequest leave = findDecidable(id);
        decide(leave, LeaveStatus.REJECTED, note);
        timeline.record(leave.getEmployee(), EmployeeEventType.LEAVE_REJECTED, leave.getFromDate(),
                leave.getLeaveType().getName() + " rejected", null,
                leave.getFromDate() + " to " + leave.getToDate(), note);
        return LeaveRequestMapper.toResponse(leave);
    }

    /** An employee withdraws their own request while it is still pending. */
    public LeaveRequestResponse cancel(Long id) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        if (!leave.getEmployee().getId().equals(scope.requireCurrentEmployeeId())) {
            throw new ResourceNotFoundException("Leave request not found: " + id);
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Only a pending request can be cancelled");
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        return LeaveRequestMapper.toResponse(leave);
    }

    private LeaveRequest findDecidable(Long id) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        if (!TenantContext.canAccess(leave.getOrganization().getId())) {
            throw new ResourceNotFoundException("Leave request not found: " + id);
        }
        scope.assertCanAccess(leave.getEmployee());
        Long approverId = scope.currentEmployeeId();
        if (approverId != null && approverId.equals(leave.getEmployee().getId())) {
            throw new AccessDeniedException("You cannot decide your own leave request");
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Leave request is already " + leave.getStatus());
        }
        return leave;
    }

    private void decide(LeaveRequest leave, LeaveStatus status, String note) {
        Long approverId = scope.currentEmployeeId();
        leave.setStatus(status);
        leave.setDecidedBy(approverId != null ? employeeRepository.findById(approverId).orElse(null) : null);
        leave.setDecidedAt(LocalDateTime.now());
        leave.setDecisionNote(note);
    }

    private LeaveBalance balanceFor(Employee employee, LeaveType leaveType, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), leaveType.getId(), year)
                .orElseGet(() -> {
                    LeaveBalance created = new LeaveBalance();
                    created.setOrganization(employee.getOrganization());
                    created.setEmployee(employee);
                    created.setLeaveType(leaveType);
                    created.setYear(year);
                    created.setEntitledDays(BigDecimal.valueOf(leaveType.getAnnualDays()));
                    created.setUsedDays(BigDecimal.ZERO);
                    return leaveBalanceRepository.save(created);
                });
    }

    /** Marks each day of an approved leave as ON_LEAVE so attendance and payroll treat it as covered. */
    private void markOnLeave(LeaveRequest leave) {
        Employee employee = leave.getEmployee();
        for (LocalDate d = leave.getFromDate(); !d.isAfter(leave.getToDate()); d = d.plusDays(1)) {
            Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), d)
                    .orElseGet(() -> null);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setOrganization(employee.getOrganization());
                attendance.setEmployee(employee);
                attendance.setWorkDate(d);
                attendance.setMethod(AttendanceMethod.MANUAL);
                attendance.setNote("Approved leave");
            } else if (attendance.getCheckInAt() != null) {
                continue;
            }
            attendance.setStatus(AttendanceStatus.ON_LEAVE);
            attendanceRepository.save(attendance);
        }
    }

    private Employee currentEmployee() {
        return employeeRepository.findById(scope.requireCurrentEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
    }
}
