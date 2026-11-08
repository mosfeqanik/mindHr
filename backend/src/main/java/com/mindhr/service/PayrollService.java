package com.mindhr.service;

import com.mindhr.dto.PayrollRunCreateRequest;
import com.mindhr.dto.PayrollRunResponse;
import com.mindhr.dto.PayslipAdjustRequest;
import com.mindhr.dto.PayslipResponse;
import com.mindhr.entity.Attendance;
import com.mindhr.entity.Branch;
import com.mindhr.entity.Employee;
import com.mindhr.entity.LeaveRequest;
import com.mindhr.entity.Organization;
import com.mindhr.entity.PayrollRun;
import com.mindhr.entity.Payslip;
import com.mindhr.entity.SalaryStructure;
import com.mindhr.entity.enums.LeaveStatus;
import com.mindhr.entity.enums.PayrollStatus;
import com.mindhr.entity.enums.PlanFeature;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.PayrollRunMapper;
import com.mindhr.mapper.PayslipMapper;
import com.mindhr.repository.AttendanceRepository;
import com.mindhr.repository.BranchRepository;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.LeaveRequestRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.PayrollRunRepository;
import com.mindhr.repository.PayslipRepository;
import com.mindhr.repository.SalaryStructureRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monthly payroll: a run generates one DRAFT payslip per active employee that has a salary
 * structure effective in the month. Rules (simple, org-agnostic defaults):
 * <ul>
 *   <li>Friday is the weekly off day; working days are the other days of the month.</li>
 *   <li>Daily rate = basic salary / working days in the month.</li>
 *   <li>A working day with no attendance record (and not on paid leave) is absent; unpaid approved
 *       leave and half days count against the salary too (half day = 0.5 absent).</li>
 *   <li>Every 3 late arrivals deduct one daily rate.</li>
 *   <li>No public-holiday calendar yet: a holiday without attendance counts as absent.</li>
 * </ul>
 * Overtime, bonus, commission, loan and advance deductions are entered by adjusting the DRAFT payslip.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private static final int LATE_ARRIVALS_PER_DEDUCTED_DAY = 3;

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final OrganizationRepository organizationRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PlanLimitService planLimitService;

    public PayrollRunResponse generate(PayrollRunCreateRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        planLimitService.assertFeature(organizationId, PlanFeature.PAYROLL);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        Branch branch = null;
        if (request.branchId() != null) {
            branch = branchRepository.findById(request.branchId())
                    .filter(b -> b.getOrganization().getId().equals(organizationId))
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.branchId()));
        }
        boolean exists = branch == null
                ? payrollRunRepository.existsByOrganizationIdAndYearAndMonthAndBranchIsNull(organizationId, request.year(), request.month())
                : payrollRunRepository.existsByOrganizationIdAndYearAndMonthAndBranchId(organizationId, request.year(), request.month(), branch.getId());
        if (exists) {
            throw new DuplicateResourceException("Payroll for " + request.year() + "-" + request.month() + " already exists");
        }

        PayrollRun run = new PayrollRun();
        run.setOrganization(organization);
        run.setYear(request.year());
        run.setMonth(request.month());
        run.setBranch(branch);
        run.setStatus(PayrollStatus.DRAFT);
        run = payrollRunRepository.save(run);

        YearMonth month = YearMonth.of(request.year(), request.month());
        Long branchId = branch != null ? branch.getId() : null;
        int skipped = 0;
        for (Employee employee : employeeRepository.findByOrganizationId(organizationId)) {
            if (!employee.isActive()) {
                continue;
            }
            if (branchId != null && (employee.getBranch() == null || !branchId.equals(employee.getBranch().getId()))) {
                continue;
            }
            SalaryStructure salary = salaryStructureRepository.findByEmployeeIdOrderByEffectiveFromDesc(employee.getId()).stream()
                    .filter(s -> !s.getEffectiveFrom().isAfter(month.atEndOfMonth()))
                    .findFirst().orElse(null);
            if (salary == null) {
                skipped++;
                continue;
            }
            payslipRepository.save(buildPayslip(run, employee, salary, month));
        }
        String note = request.note() != null ? request.note() : "";
        if (skipped > 0) {
            note = (note + " " + skipped + " employee(s) skipped: no salary structure.").trim();
        }
        run.setNote(note.isEmpty() ? null : note);
        return PayrollRunMapper.toResponse(run);
    }

    public PayslipResponse adjust(Long payslipId, PayslipAdjustRequest request) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + payslipId));
        if (!TenantContext.canAccess(payslip.getOrganization().getId())) {
            throw new ResourceNotFoundException("Payslip not found: " + payslipId);
        }
        if (payslip.getStatus() != PayrollStatus.DRAFT) {
            throw new IllegalArgumentException("Only a DRAFT payslip can be adjusted");
        }
        if (request.overtime() != null) payslip.setOvertime(request.overtime());
        if (request.bonus() != null) payslip.setBonus(request.bonus());
        if (request.commission() != null) payslip.setCommission(request.commission());
        if (request.loanDeduction() != null) payslip.setLoanDeduction(request.loanDeduction());
        if (request.advanceDeduction() != null) payslip.setAdvanceDeduction(request.advanceDeduction());
        recalculate(payslip);
        return PayslipMapper.toResponse(payslip);
    }

    public PayrollRunResponse finalizeRun(Long runId) {
        PayrollRun run = findRun(runId);
        requireStatus(run, PayrollStatus.DRAFT, "finalized");
        payslipRepository.findByPayrollRunId(runId).forEach(p -> p.setStatus(PayrollStatus.FINALIZED));
        run.setStatus(PayrollStatus.FINALIZED);
        return PayrollRunMapper.toResponse(run);
    }

    public PayrollRunResponse markPaid(Long runId) {
        PayrollRun run = findRun(runId);
        requireStatus(run, PayrollStatus.FINALIZED, "paid");
        payslipRepository.findByPayrollRunId(runId).forEach(p -> p.setStatus(PayrollStatus.PAID));
        run.setStatus(PayrollStatus.PAID);
        return PayrollRunMapper.toResponse(run);
    }

    /** Only a DRAFT run can be thrown away (and regenerated). */
    public void deleteDraft(Long runId) {
        PayrollRun run = findRun(runId);
        if (run.getStatus() != PayrollStatus.DRAFT) {
            throw new IllegalArgumentException("Only a DRAFT payroll run can be deleted");
        }
        payslipRepository.deleteAll(payslipRepository.findByPayrollRunId(runId));
        payrollRunRepository.delete(run);
    }

    private Payslip buildPayslip(PayrollRun run, Employee employee, SalaryStructure salary, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = end.isAfter(today) ? today : end;
        LocalDate from = employee.getJoinedAt() != null && employee.getJoinedAt().isAfter(start) ? employee.getJoinedAt() : start;

        int workingDaysInMonth = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.FRIDAY) {
                workingDaysInMonth++;
            }
        }
        workingDaysInMonth = Math.max(workingDaysInMonth, 1);

        Map<LocalDate, Attendance> byDate = new HashMap<>();
        for (Attendance a : attendanceRepository.findByEmployeeIdAndWorkDateBetween(employee.getId(), start, end)) {
            byDate.put(a.getWorkDate(), a);
        }
        Set<LocalDate> unpaidLeave = unpaidLeaveDates(employee, start, end);

        int absent = 0;
        int unpaid = 0;
        int late = 0;
        int halfDays = 0;
        int present = 0;
        for (LocalDate d = from; !d.isAfter(cutoff); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.FRIDAY) {
                continue;
            }
            if (unpaidLeave.contains(d)) {
                unpaid++;
                continue;
            }
            Attendance a = byDate.get(d);
            if (a == null) {
                absent++;
                continue;
            }
            switch (a.getStatus()) {
                case PRESENT -> present++;
                case LATE -> {
                    present++;
                    late++;
                }
                case HALF_DAY -> halfDays++;
                case ABSENT -> absent++;
                case ON_LEAVE -> {
                }
            }
        }

        BigDecimal basic = salary.getBasicSalary();
        BigDecimal perDay = basic.divide(BigDecimal.valueOf(workingDaysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal absentEquivalent = BigDecimal.valueOf(absent + unpaid)
                .add(BigDecimal.valueOf(halfDays).multiply(new BigDecimal("0.5")));

        Payslip payslip = new Payslip();
        payslip.setOrganization(run.getOrganization());
        payslip.setPayrollRun(run);
        payslip.setEmployee(employee);
        payslip.setBasicSalary(basic);
        payslip.setAllowances(salary.getAllowances());
        payslip.setAbsentDeduction(perDay.multiply(absentEquivalent).setScale(2, RoundingMode.HALF_UP).min(basic));
        payslip.setLateDeduction(perDay.multiply(BigDecimal.valueOf(late / LATE_ARRIVALS_PER_DEDUCTED_DAY))
                .setScale(2, RoundingMode.HALF_UP));
        payslip.setDaysPresent(present);
        payslip.setDaysAbsent(absent + unpaid);
        payslip.setDaysLate(late);
        payslip.setStatus(PayrollStatus.DRAFT);
        recalculate(payslip);
        return payslip;
    }

    private Set<LocalDate> unpaidLeaveDates(Employee employee, LocalDate start, LocalDate end) {
        Set<LocalDate> dates = new HashSet<>();
        List<LeaveRequest> approved = leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.APPROVED);
        for (LeaveRequest leave : approved) {
            if (leave.getLeaveType().isPaid()) {
                continue;
            }
            for (LocalDate d = leave.getFromDate(); !d.isAfter(leave.getToDate()); d = d.plusDays(1)) {
                if (!d.isBefore(start) && !d.isAfter(end)) {
                    dates.add(d);
                }
            }
        }
        return dates;
    }

    /** gross = basic + allowances + overtime + bonus + commission; net = gross - deductions, never below zero. */
    private void recalculate(Payslip p) {
        BigDecimal gross = p.getBasicSalary().add(p.getAllowances()).add(p.getOvertime())
                .add(p.getBonus()).add(p.getCommission());
        BigDecimal net = gross.subtract(p.getLateDeduction()).subtract(p.getAbsentDeduction())
                .subtract(p.getLoanDeduction()).subtract(p.getAdvanceDeduction());
        p.setGrossSalary(gross);
        p.setNetSalary(net.max(BigDecimal.ZERO));
    }

    private PayrollRun findRun(Long id) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + id));
        if (!TenantContext.canAccess(run.getOrganization().getId())) {
            throw new ResourceNotFoundException("Payroll run not found: " + id);
        }
        return run;
    }

    private void requireStatus(PayrollRun run, PayrollStatus required, String action) {
        if (run.getStatus() != required) {
            throw new IllegalArgumentException("Payroll run must be " + required + " to be " + action
                    + " (it is " + run.getStatus() + ")");
        }
    }
}
