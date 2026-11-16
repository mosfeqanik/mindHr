package com.mindhr.mapper;

import com.mindhr.dto.LeaveTypeResponse;
import com.mindhr.entity.LeaveType;

public final class LeaveTypeMapper {

    private LeaveTypeMapper() {
    }

    public static LeaveTypeResponse toResponse(LeaveType entity) {
        return new LeaveTypeResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getName(),
                entity.getAnnualDays(),
                entity.isPaid(),
                entity.isActive()
        );
    }
}
