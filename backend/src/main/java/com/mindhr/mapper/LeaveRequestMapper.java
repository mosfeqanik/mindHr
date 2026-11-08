package com.mindhr.mapper;

import com.mindhr.dto.LeaveRequestResponse;
import com.mindhr.entity.LeaveRequest;

public final class LeaveRequestMapper {

    private LeaveRequestMapper() {
    }

    public static LeaveRequestResponse toResponse(LeaveRequest entity) {
        return new LeaveRequestResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getLeaveType().getId(),
                entity.getFromDate(),
                entity.getToDate(),
                entity.getDays(),
                entity.getReason(),
                entity.getStatus(),
                entity.getDecidedBy() != null ? entity.getDecidedBy().getId() : null,
                entity.getDecidedAt(),
                entity.getDecisionNote()
        );
    }
}
