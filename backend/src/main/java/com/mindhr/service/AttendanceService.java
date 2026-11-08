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
public class AttendanceService {

    private final AttendanceRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<AttendanceResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(AttendanceMapper::toResponse)
                .toList();
    }

    public AttendanceResponse getById(Long id) {
        return AttendanceMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public AttendanceResponse create(AttendanceRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        Attendance entity = new Attendance();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        Attendance saved = repository.save(entity);
        return AttendanceMapper.toResponse(saved);
    }

    @Transactional
    public AttendanceResponse update(Long id, AttendanceRequest request) {
        Attendance entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return AttendanceMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(Attendance entity, AttendanceRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setWorkDate(request.workDate());
        entity.setCheckInAt(request.checkInAt());
        entity.setCheckOutAt(request.checkOutAt());
        entity.setCheckInLatitude(request.checkInLatitude());
        entity.setCheckInLongitude(request.checkInLongitude());
        entity.setCheckOutLatitude(request.checkOutLatitude());
        entity.setCheckOutLongitude(request.checkOutLongitude());
        entity.setMethod(request.method() == null ? AttendanceMethod.MANUAL : request.method());
        entity.setStatus(request.status() == null ? AttendanceStatus.PRESENT : request.status());
        entity.setLateMinutes(request.lateMinutes() == null ? 0 : request.lateMinutes());
        entity.setWorkedMinutes(request.workedMinutes() == null ? 0 : request.workedMinutes());
        entity.setNote(request.note());
    }

    private Attendance findAccessibleEntity(Long id) {
        Attendance entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("Attendance not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}
