package com.bookngo.authservice.dto.response;

import com.bookngo.authservice.model.enums.UserRole;
import com.bookngo.authservice.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicturePath;
    private UserRole role;
    private UserStatus status;
}