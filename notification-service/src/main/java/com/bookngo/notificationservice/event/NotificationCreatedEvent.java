package com.bookngo.notificationservice.event;

import com.bookngo.notificationservice.dto.response.NotificationResponse;

public record NotificationCreatedEvent(
        Long userId,
        String type,
        NotificationResponse notification
) {}
