package com.project.bookngo.model.response;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ForgotPasswordResponse {
    private String message;
}
