package com.project.bookngo.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.project.bookngo.enums.SessionStatus;

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