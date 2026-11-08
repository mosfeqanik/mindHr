package com.mindhr.mapper;

import com.mindhr.dto.NotificationResponse;
import com.mindhr.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrganization().getId(),
                notification.getUser().getId(),
                notification.getChannel(),
                notification.getSubject(),
                notification.getBody(),
                notification.getSentAt(),
                notification.isRead()
        );
    }
}
