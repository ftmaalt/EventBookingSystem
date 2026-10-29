package com.project.bookngo.model.response;

import java.time.LocalDateTime;

/** A customer's waiting entry with display information and its current FIFO position. */
public record WaitlistedSessionResponse(
        Long entryId, Long sessionId, Long activityId, String activityTitle,
        LocalDateTime startTime, LocalDateTime endTime, LocalDateTime joinedAt, Long position) {
}
