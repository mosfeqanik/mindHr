package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationSettingRequest(

        /** Only read for platform staff, who must operate on an explicit organization; ignored for an org user. */
        Long organizationId,

        @NotBlank(message = "Key is required")
        @Size(max = 100, message = "Key must be at most 100 characters")
        String key,

        String value
) {
}
