package com.mindhr.mapper;

import com.mindhr.dto.ActivityLogResponse;
import com.mindhr.entity.ActivityLog;

public final class ActivityLogMapper {

    private ActivityLogMapper() {
    }

    public static ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getOrganization() != null ? log.getOrganization().getId() : null,
                log.getActor() != null ? log.getActor().getId() : null,
                log.getActor() != null ? log.getActor().getFullName() : null,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
