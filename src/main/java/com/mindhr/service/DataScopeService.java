package com.mindhr.service;

import com.mindhr.entity.Employee;
import com.mindhr.entity.enums.DataScope;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.security.JwtAuthenticatedPrincipal;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Row-level visibility inside an organization, driven by the caller's {@link DataScope}:
 * ORGANIZATION sees every employee, BRANCH their branch, TEAM themselves and their direct
 * reports, SELF only themselves. Platform staff are unrestricted here (organization isolation
 * is still enforced by {@link TenantContext}).
 *
 * <p>The scope, employee and branch come from the JWT, so a role or branch change takes effect
 * at the user's next login.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataScopeService {

    private final EmployeeRepository employeeRepository;

    public DataScope scope() {
        DataScope scope = TenantContext.currentPrincipal().dataScope();
        return scope != null ? scope : DataScope.ORGANIZATION;
    }

    /** The caller's own employee id, or null for users without an employee record. */
    public Long currentEmployeeId() {
        return TenantContext.currentPrincipal().employeeId();
    }

    /** Like {@link #currentEmployeeId()} but fails when the caller has no employee profile. */
    public Long requireCurrentEmployeeId() {
        Long id = currentEmployeeId();
        if (id == null) {
            throw new IllegalArgumentException("This account has no employee profile");
        }
        return id;
    }

    /** Employee ids the caller may see in the organization; {@code null} means no restriction. */
    public Set<Long> visibleEmployeeIds(Long organizationId) {
        JwtAuthenticatedPrincipal principal = TenantContext.currentPrincipal();
        Long self = principal.employeeId();
        Set<Long> ids = new HashSet<>();
        switch (scope()) {
            case PLATFORM, ORGANIZATION -> {
                return null;
            }
            case BRANCH -> {
                if (principal.branchId() != null) {
                    employeeRepository.findByOrganizationIdAndBranchId(organizationId, principal.branchId())
                            .forEach(e -> ids.add(e.getId()));
                }
            }
            case TEAM -> {
                if (self != null) {
                    employeeRepository.findByManagerId(self).forEach(e -> ids.add(e.getId()));
                }
            }
            case SELF -> {
            }
        }
        if (self != null) {
            ids.add(self);
        }
        return ids;
    }

    public boolean canAccess(Employee employee) {
        if (!TenantContext.canAccess(employee.getOrganization().getId())) {
            return false;
        }
        Set<Long> visible = visibleEmployeeIds(employee.getOrganization().getId());
        return visible == null || visible.contains(employee.getId());
    }

    /** Hides records outside the caller's scope as "not found" rather than "forbidden". */
    public void assertCanAccess(Employee employee) {
        if (!canAccess(employee)) {
            throw new ResourceNotFoundException("Employee not found: " + employee.getId());
        }
    }

    /**
     * Resolves the employee a create/update targets. A SELF caller always acts on themselves;
     * anyone else must name a visible employee of {@code organizationId}.
     */
    public Employee resolveEmployeeForWrite(Long requestedEmployeeId, Long organizationId) {
        Long employeeId = requestedEmployeeId;
        if (scope() == DataScope.SELF) {
            employeeId = requireCurrentEmployeeId();
            if (requestedEmployeeId != null && !requestedEmployeeId.equals(employeeId)) {
                throw new ResourceNotFoundException("Employee not found: " + requestedEmployeeId);
            }
        }
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee is required");
        }
        Long id = employeeId;
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        if (!employee.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Employee not found: " + id);
        }
        assertCanAccess(employee);
        return employee;
    }
}
