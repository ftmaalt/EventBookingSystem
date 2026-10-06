package com.project.bookngo.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Setter
@Getter
public class ProviderProfileRequest {
    @NotBlank
    @Max(8)
    private String phone;

    private String description;

}
