package com.project.bookngo.model.response;

import java.time.LocalDateTime;

public record ApiErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
){}
