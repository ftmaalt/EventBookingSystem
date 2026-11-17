package com.bookngo.authservice.dto.request;

import com.bookngo.authservice.model.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {
    @NotNull
    private UserStatus status;
}
