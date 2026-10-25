package com.project.bookngo.service;


import com.project.bookngo.model.User;
import com.project.bookngo.model.request.UserProfileRequest;
import com.project.bookngo.model.response.UserProfileResponse;
import com.project.bookngo.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class UserProfileService {


    private final UsersRepository usersRepository;

    private final FileStorageService fileStorageService;

    private final AuditLogService auditLogService;

    private final CurrentUserService currentUserService;

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    public UserProfileService(UsersRepository usersRepository, FileStorageService fileStorageService, AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.usersRepository = usersRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }


    public UserProfileResponse getMyProfile() {
        logger.info("Fetching current user's profile");
        return toResponse(currentUserService.getCurrentUser());
    }

    public UserProfileResponse updateMyProfile(UserProfileRequest request) {
        logger.info("Updating current user's profile");
        User user = currentUserService.getCurrentUser();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        auditLogService.logAction("PROFILE_UPDATED", currentUserService.getCurrentUser().getEmail(), "User", user.getId(), "User profile was updated");
        User saved = usersRepository.save(user);
        logger.info("Current user's profile updated successfully");
        return toResponse(saved);
    }

    public UserProfileResponse uploadProfilePicture(MultipartFile file) {
        logger.info("Uploading profile picture for current user");
        User user = currentUserService.getCurrentUser();
        String path = fileStorageService.storeProfilePicture(file);
        user.setProfilePicturePath(path);
        User saved = usersRepository.save(user);
        logger.info("Profile picture uploaded and profile updated successfully");
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getProfilePicturePath(),
                user.getRole(), user.getStatus()
        );
    }
}