package com.bookngo.bookingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.bookngo.bookingservice.model.enums.SessionStatus;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class SessionResponse {
    private Long id;
    private Long activityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer spotsLeft;
    private SessionStatus status;
}