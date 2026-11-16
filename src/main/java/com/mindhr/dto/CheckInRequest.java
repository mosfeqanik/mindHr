package com.mindhr.dto;

import com.mindhr.entity.enums.AttendanceMethod;

/** Body for the mobile check-in / check-out endpoints. Location is required when the branch has a geofence. */
public record CheckInRequest(
        /** Defaults to MOBILE_GPS. */
        AttendanceMethod method,
        Double latitude,
        Double longitude,
        String note
) {
}
