package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for a signed-in user changing their own password. */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "New password must be 8 to 100 characters")
        String newPassword
) {
}
