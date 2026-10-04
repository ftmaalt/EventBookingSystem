package com.project.bookngo.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    private String fullname;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min=8)
    private String password;

    @NotBlank
    private String phone;
}
