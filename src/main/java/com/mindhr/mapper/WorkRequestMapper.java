package com.mindhr.mapper;

import com.mindhr.dto.WorkRequestResponse;
import com.mindhr.entity.WorkRequest;

public final class WorkRequestMapper {

    private WorkRequestMapper() {
    }

    public static WorkRequestResponse toResponse(WorkRequest entity) {
        return new WorkRequestResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getDetails(),
                entity.getAmount(),
                entity.getRequestDate(),
                entity.getStatus(),
                entity.getDecidedBy() != null ? entity.getDecidedBy().getId() : null,
                entity.getDecidedAt(),
                entity.getDecisionNote()
        );
    }
}
