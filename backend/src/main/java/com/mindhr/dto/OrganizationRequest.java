package com.mindhr.dto;

import com.mindhr.entity.enums.OrganizationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record OrganizationRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotNull(message = "Status is required")
        OrganizationStatus status,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        LocalDateTime trialEndsAt,

        /** Required on create: the organization starts with a subscription to this plan. Ignored on update. */
        Long planId
) {
}
