package com.mindhr.dto;

import com.mindhr.entity.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserRequest(

        /** Null for a platform account (Super Admin / SaaS Staff); required for an org account. */
        Long organizationId,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        /** Required on create; leave null on update to keep the current password. */
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @NotNull(message = "User type is required")
        UserType userType,

        Boolean active,

        Set<Long> roleIds
) {
}
