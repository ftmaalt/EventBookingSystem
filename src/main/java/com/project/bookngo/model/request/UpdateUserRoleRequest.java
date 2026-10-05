package com.project.bookngo.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.project.bookngo.model.enums.UserRole;

@Getter
@Setter
public class UpdateUserRoleRequest {
    @NotNull
    private UserRole role;
}
