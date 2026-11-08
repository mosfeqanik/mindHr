package com.mindhr.service;

import com.mindhr.dto.*;
import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import com.mindhr.exception.*;
import com.mindhr.mapper.*;
import com.mindhr.repository.*;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayslipService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<PayslipResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .filter(e -> scope.scope() != DataScope.SELF || e.getStatus() != PayrollStatus.DRAFT)
                .map(PayslipMapper::toResponse)
                .toList();
    }

    public PayslipResponse getById(Long id) {
        return PayslipMapper.toResponse(findAccessibleEntity(id));
    }

    private void applyRequest(Payslip entity, PayslipRequest request, Long organizationId) {
        entity.setPayrollRun(resolvePayrollRun(request.payrollRunId(), organizationId, true));
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setBasicSalary(request.basicSalary() == null ? BigDecimal.ZERO : request.basicSalary());
        entity.setAllowances(request.allowances() == null ? BigDecimal.ZERO : request.allowances());
        entity.setOvertime(request.overtime() == null ? BigDecimal.ZERO : request.overtime());
        entity.setBonus(request.bonus() == null ? BigDecimal.ZERO : request.bonus());
        entity.setCommission(request.commission() == null ? BigDecimal.ZERO : request.commission());
        entity.setGrossSalary(request.grossSalary() == null ? BigDecimal.ZERO : request.grossSalary());
        entity.setLateDeduction(request.lateDeduction() == null ? BigDecimal.ZERO : request.lateDeduction());
        entity.setAbsentDeduction(request.absentDeduction() == null ? BigDecimal.ZERO : request.absentDeduction());
        entity.setLoanDeduction(request.loanDeduction() == null ? BigDecimal.ZERO : request.loanDeduction());
        entity.setAdvanceDeduction(request.advanceDeduction() == null ? BigDecimal.ZERO : request.advanceDeduction());
        entity.setNetSalary(request.netSalary() == null ? BigDecimal.ZERO : request.netSalary());
        entity.setDaysPresent(request.daysPresent() == null ? 0 : request.daysPresent());
        entity.setDaysAbsent(request.daysAbsent() == null ? 0 : request.daysAbsent());
        entity.setDaysLate(request.daysLate() == null ? 0 : request.daysLate());
        entity.setStatus(request.status() == null ? PayrollStatus.DRAFT : request.status());
    }

    private PayrollRun resolvePayrollRun(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("PayrollRun is required");
            }
            return null;
        }
        PayrollRun ref = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("PayrollRun not found: " + id);
        }
        return ref;
    }

    private Payslip findAccessibleEntity(Long id) {
        Payslip entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("Payslip not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        if (scope.scope() == DataScope.SELF && entity.getStatus() == PayrollStatus.DRAFT) { throw new ResourceNotFoundException("Payslip not found: " + id); }
        return entity;
    }
}
