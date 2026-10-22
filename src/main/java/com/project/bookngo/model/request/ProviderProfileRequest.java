package com.project.bookngo.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProviderProfileRequest {
    @NotBlank
    @Pattern(regexp = "^\\d{8}$", message = "Phone number must be exactly 8 digits")
    private String phone;

    private String description;

}
