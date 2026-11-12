package com.bookngo.bookingservice.dto.response;

import com.bookngo.bookingservice.model.enums.WaitListStatus;

import java.time.LocalDateTime;

public record WaitListResponse(
        Long entryId,
        Long sessionId,
        WaitListStatus status,
        LocalDateTime joinedAt,
        Long position

) {
}
