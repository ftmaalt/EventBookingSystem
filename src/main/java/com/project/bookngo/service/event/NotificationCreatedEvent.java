package com.project.bookngo.service.event;

import com.project.bookngo.model.response.NotificationResponse;

public record NotificationCreatedEvent(
        Long userId,
        String type,
        NotificationResponse notification
) {}
