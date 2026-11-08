package com.mindhr.service;

import com.mindhr.dto.WorkRequestCreateRequest;
import com.mindhr.dto.WorkRequestResponse;
import com.mindhr.entity.Employee;
import com.mindhr.entity.WorkRequest;
import com.mindhr.entity.enums.RequestStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.WorkRequestMapper;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.WorkRequestRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Employee requests (advance, loan, shift change, expense, ...): created by the employee, decided by a manager. */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkRequestActionService {

    private final WorkRequestRepository workRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final DataScopeService scope;

    public WorkRequestResponse create(WorkRequestCreateRequest request) {
        Employee employee = employeeRepository.findById(scope.requireCurrentEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        WorkRequest workRequest = new WorkRequest();
        workRequest.setOrganization(employee.getOrganization());
        workRequest.setEmployee(employee);
        workRequest.setType(request.type());
        workRequest.setTitle(request.title());
        workRequest.setDetails(request.details());
        workRequest.setAmount(request.amount());
        workRequest.setRequestDate(request.requestDate() != null ? request.requestDate() : LocalDate.now());
        workRequest.setStatus(RequestStatus.PENDING);
        return WorkRequestMapper.toResponse(workRequestRepository.save(workRequest));
    }

    public WorkRequestResponse approve(Long id, String note) {
        return decide(id, RequestStatus.APPROVED, note);
    }

    public WorkRequestResponse reject(Long id, String note) {
        return decide(id, RequestStatus.REJECTED, note);
    }

    public WorkRequestResponse cancel(Long id) {
        WorkRequest workRequest = find(id);
        if (!workRequest.getEmployee().getId().equals(scope.requireCurrentEmployeeId())) {
            throw new ResourceNotFoundException("Request not found: " + id);
        }
        if (workRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Only a pending request can be cancelled");
        }
        workRequest.setStatus(RequestStatus.CANCELLED);
        return WorkRequestMapper.toResponse(workRequest);
    }

    private WorkRequestResponse decide(Long id, RequestStatus status, String note) {
        WorkRequest workRequest = find(id);
        scope.assertCanAccess(workRequest.getEmployee());
        Long approverId = scope.currentEmployeeId();
        if (approverId != null && approverId.equals(workRequest.getEmployee().getId())) {
            throw new AccessDeniedException("You cannot decide your own request");
        }
        if (workRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Request is already " + workRequest.getStatus());
        }
        workRequest.setStatus(status);
        workRequest.setDecidedBy(approverId != null ? employeeRepository.findById(approverId).orElse(null) : null);
        workRequest.setDecidedAt(LocalDateTime.now());
        workRequest.setDecisionNote(note);
        return WorkRequestMapper.toResponse(workRequest);
    }

    private WorkRequest find(Long id) {
        WorkRequest workRequest = workRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + id));
        if (!TenantContext.canAccess(workRequest.getOrganization().getId())) {
            throw new ResourceNotFoundException("Request not found: " + id);
        }
        return workRequest;
    }
}
