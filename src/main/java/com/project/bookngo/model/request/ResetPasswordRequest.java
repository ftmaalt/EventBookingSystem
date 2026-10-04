package com.project.bookngo.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min =8)
    private String newPassword;
}
