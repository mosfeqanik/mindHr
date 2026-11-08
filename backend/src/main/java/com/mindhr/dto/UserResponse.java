package com.mindhr.dto;

import com.mindhr.entity.enums.UserType;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        Long organizationId,
        String email,
        String fullName,
        String phone,
        UserType userType,
        boolean active,
        LocalDateTime lastLoginAt,
        Set<RoleSummary> roles
) {
}
