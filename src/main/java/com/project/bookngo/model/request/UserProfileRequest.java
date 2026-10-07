package com.project.bookngo.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {
    @NotBlank
    private String fullName;

    private String phone;
}