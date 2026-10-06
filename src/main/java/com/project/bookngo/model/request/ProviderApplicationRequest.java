package com.project.bookngo.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderApplicationRequest {
    @NotBlank
    private String businessName;

    @NotBlank
    private String contactName;

    @NotBlank
    @Max(8)
    private String phone;

    @NotBlank
    private String city;

    private String description;

    @NotBlank
    private String proposedActivities;
}
