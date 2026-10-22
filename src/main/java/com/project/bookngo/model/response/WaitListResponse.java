package com.project.bookngo.model.response;

import com.project.bookngo.model.enums.WaitListStatus;

import java.time.LocalDateTime;

public record WaitListResponse(
        Long entryId,
        Long sessionId,
        WaitListStatus status,
        LocalDateTime joinedAt,
        Long position

) {
}
