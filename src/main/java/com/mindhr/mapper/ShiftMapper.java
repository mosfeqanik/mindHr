package com.mindhr.mapper;

import com.mindhr.dto.ShiftResponse;
import com.mindhr.entity.Shift;

public final class ShiftMapper {

    private ShiftMapper() {
    }

    public static ShiftResponse toResponse(Shift entity) {
        return new ShiftResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getName(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isActive()
        );
    }
}
