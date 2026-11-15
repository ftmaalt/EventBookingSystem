package com.bookngo.authservice.dto.request;

import com.bookngo.authservice.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdateUserRoleRequest {
    @NotNull
    private UserRole role;
}
