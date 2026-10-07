package com.project.bookngo.model.response;

import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.enums.UserStatus;
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