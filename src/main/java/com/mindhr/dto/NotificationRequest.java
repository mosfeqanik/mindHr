package com.mindhr.dto;

import com.mindhr.entity.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(

        Long organizationId,

        @NotNull(message = "Recipient user is required")
        Long userId,

        @NotNull(message = "Channel is required")
        NotificationChannel channel,

        @Size(max = 200, message = "Subject must be at most 200 characters")
        String subject,

        @NotBlank(message = "Body is required")
        String body
) {
}
