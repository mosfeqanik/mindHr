package com.mindhr.dto;

import com.mindhr.entity.enums.NotificationChannel;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long organizationId,
        Long userId,
        NotificationChannel channel,
        String subject,
        String body,
        LocalDateTime sentAt,
        boolean read
) {
}
