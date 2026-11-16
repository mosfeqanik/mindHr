package com.mindhr.mapper;

import com.mindhr.dto.EmployeeEventResponse;
import com.mindhr.entity.EmployeeEvent;

public final class EmployeeEventMapper {

    private EmployeeEventMapper() {
    }

    public static EmployeeEventResponse toResponse(EmployeeEvent entity) {
        return new EmployeeEventResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getType(),
                entity.getEventDate(),
                entity.getTitle(),
                entity.getDetails(),
                entity.getOldValue(),
                entity.getNewValue()
        );
    }
}
