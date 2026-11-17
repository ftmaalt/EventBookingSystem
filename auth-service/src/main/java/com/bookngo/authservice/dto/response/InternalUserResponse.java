package com.bookngo.authservice.dto.response;

import com.bookngo.authservice.model.enums.UserRole;
import com.bookngo.authservice.model.enums.UserStatus;
// the info other services can know about the user
public record InternalUserResponse(Long id, String email, UserRole role, UserStatus status) {}
