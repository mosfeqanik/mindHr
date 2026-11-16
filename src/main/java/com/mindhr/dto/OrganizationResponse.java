package com.mindhr.dto;

import com.mindhr.entity.enums.OrganizationStatus;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String slug,
        OrganizationStatus status,
        String email,
        String phone,
        String address,
        LocalDateTime trialEndsAt,
        LocalDateTime activatedAt,
        Long ownerUserId,
        String ownerName,
        String ownerEmail
) {
}
