package com.mindhr.mapper;

import com.mindhr.dto.AttendanceResponse;
import com.mindhr.entity.Attendance;

public final class AttendanceMapper {

    private AttendanceMapper() {
    }

    public static AttendanceResponse toResponse(Attendance entity) {
        return new AttendanceResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getWorkDate(),
                entity.getCheckInAt(),
                entity.getCheckOutAt(),
                entity.getCheckInLatitude(),
                entity.getCheckInLongitude(),
                entity.getCheckOutLatitude(),
                entity.getCheckOutLongitude(),
                entity.getMethod(),
                entity.getStatus(),
                entity.getLateMinutes(),
                entity.getWorkedMinutes(),
                entity.getNote()
        );
    }
}
