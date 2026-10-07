package com.project.bookngo.controller;

import com.project.bookngo.model.request.UserProfileRequest;
import com.project.bookngo.model.response.UserProfileResponse;
import com.project.bookngo.service.UserProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserProfileService userProfileService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        logger.info("Calling getMyProfile ===>");
        return ResponseEntity.ok(userProfileService.getMyProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UserProfileRequest request) {
        logger.info("Calling updateMyProfile ===>");
        return ResponseEntity.ok(userProfileService.updateMyProfile(request));
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        logger.info("Calling uploadProfilePicture ===>");
        return ResponseEntity.ok(userProfileService.uploadProfilePicture(file));
    }
}