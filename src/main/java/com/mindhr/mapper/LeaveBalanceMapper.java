package com.mindhr.mapper;

import com.mindhr.dto.LeaveBalanceResponse;
import com.mindhr.entity.LeaveBalance;

public final class LeaveBalanceMapper {

    private LeaveBalanceMapper() {
    }

    public static LeaveBalanceResponse toResponse(LeaveBalance entity) {
        return new LeaveBalanceResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getLeaveType().getId(),
                entity.getYear(),
                entity.getEntitledDays(),
                entity.getUsedDays()
        );
    }
}
