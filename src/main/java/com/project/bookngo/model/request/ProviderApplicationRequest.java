package com.project.bookngo.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderApplicationRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Phone is required")
    @Size(min = 8, max = 8, message = "Phone number must be exactly 8 digits")
    @Pattern(regexp = "\\d{8}", message = "Phone number must contain only digits")
    private String phone;

    @NotBlank(message = "City is required")
    private String city;

    private String description;

    @NotBlank(message = "Proposed activities are required")
    private String proposedActivities;
}