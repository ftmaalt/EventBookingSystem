package com.bookngo.authservice.dto.response;

import com.bookngo.authservice.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class ProviderApplicationResponse {
    private Long application_id;
    private String businessName;
    private String contactName;
    private String reviewNote;
    private String city;
    private LocalDateTime createdAt;
    private String proposedActivities;
    private ApplicationStatus status;
}
