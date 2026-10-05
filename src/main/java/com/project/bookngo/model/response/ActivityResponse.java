package com.project.bookngo.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.project.bookngo.enums.ActivityStatus;

@AllArgsConstructor
@Getter
public class ActivityResponse {
    private Long activity_id;

    private String title;
    private String description;
    private BigDecimal pricePerPerson;
    private Integer durationMinutes;

    private Long categoryId;
    private Long locationId;
    private Long providerId;

    private ActivityStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
