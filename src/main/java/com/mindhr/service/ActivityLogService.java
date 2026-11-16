package com.mindhr.service;

import com.mindhr.dto.ActivityLogRequest;
import com.mindhr.dto.ActivityLogResponse;
import com.mindhr.entity.ActivityLog;
import com.mindhr.entity.Organization;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.ActivityLogMapper;
import com.mindhr.repository.ActivityLogRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import com.mindhr.entity.enums.DataScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Immutable audit trail: writable only by platform staff, never updated or deleted through the API. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    static final int DEFAULT_LIMIT = 100;
    static final int MAX_LIMIT = 500;

    /**
     * Newest first, at most {@code limit} rows. Platform staff see every organization (or one, when
     * {@code organizationId} is given). Org users see only their own organization, and only with
     * ORGANIZATION data scope (owner, HR admin): the log names who changed which record, which a
     * branch manager or employee should not see for the whole company.
     */
    public List<ActivityLogResponse> list(Long requestedOrganizationId, Integer limit) {
        PageRequest page = PageRequest.of(0, limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT));
        List<ActivityLog> logs;
        if (TenantContext.isPlatformStaff()) {
            logs = requestedOrganizationId != null
                    ? activityLogRepository.findByOrganizationIdOrderByIdDesc(requestedOrganizationId, page)
                    : activityLogRepository.findAllByOrderByIdDesc(page);
        } else {
            if (TenantContext.currentPrincipal().dataScope() != DataScope.ORGANIZATION) {
                throw new AccessDeniedException("Only organization-wide roles can view the activity log");
            }
            Long ownOrganizationId = TenantContext.resolveOrganizationId(null);
            logs = activityLogRepository.findByOrganizationIdOrderByIdDesc(ownOrganizationId, page);
        }
        return logs.stream().map(ActivityLogMapper::toResponse).toList();
    }

    /** Called by {@code ActivityRecorder} after every successful write request; no access check. */
    @Transactional
    public void record(Long organizationId, Long actorUserId, String action, String entityType, Long entityId,
                       String details, String ipAddress) {
        ActivityLog log = new ActivityLog();
        if (organizationId != null) {
            log.setOrganization(organizationRepository.getReferenceById(organizationId));
        }
        if (actorUserId != null) {
            log.setActor(userRepository.getReferenceById(actorUserId));
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLogResponse create(ActivityLogRequest request) {
        TenantContext.requirePlatformStaff();

        ActivityLog log = new ActivityLog();
        if (request.organizationId() != null) {
            Organization organization = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.organizationId()));
            log.setOrganization(organization);
        }
        if (request.actorUserId() != null) {
            userRepository.findById(request.actorUserId()).ifPresent(log::setActor);
        }
        log.setAction(request.action());
        log.setEntityType(request.entityType());
        log.setEntityId(request.entityId());
        log.setDetails(request.details());
        log.setIpAddress(request.ipAddress());

        return ActivityLogMapper.toResponse(activityLogRepository.save(log));
    }
}
