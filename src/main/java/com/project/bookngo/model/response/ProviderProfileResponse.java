package com.project.bookngo.model.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class ProviderProfileResponse {
    private Long provider_id;
    private String businessName;
    private String phone;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
