package com.bookngo.authservice.service;


import com.bookngo.authservice.dto.request.UserProfileRequest;
import com.bookngo.authservice.dto.response.UserProfileResponse;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;


@Service
public class UserProfileService {

    private final UsersRepository usersRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    private static final Logger logger =
            LoggerFactory.getLogger(UserProfileService.class);

    public UserProfileService(
            UsersRepository usersRepository,
            FileStorageService fileStorageService,
            AuditLogService auditLogService,
            CurrentUserService currentUserService
    ) {
        this.usersRepository = usersRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        logger.info("Fetching current user's profile");

        return toResponse(currentUserService.getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileRequest request) {
        logger.info("Updating current user's profile");

        User user = currentUserService.getCurrentUser();

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        User saved = usersRepository.save(user);

        auditLogService.logAction(
                "PROFILE_UPDATED",
                user.getEmail(),
                "User",
                user.getId(),
                "User profile was updated"
        );

        logger.info("Current user's profile updated successfully");

        return toResponse(saved);
    }

    @Transactional
    public UserProfileResponse uploadProfilePicture(MultipartFile file) {
        logger.info("Uploading profile picture for current user");
        User user = currentUserService.getCurrentUser();

        String oldPicturePath = user.getProfilePicturePath();
        String newPicturePath = fileStorageService.storeProfilePicture(file);

        user.setProfilePicturePath(newPicturePath);
        User saved = usersRepository.save(user);

        auditLogService.logAction("PROFILE_PICTURE_UPDATED", user.getEmail(), "User", user.getId(), "Profile picture updated");
        if (oldPicturePath != null && !oldPicturePath.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorageService.deleteProfilePicture(oldPicturePath);
                }
            });
        }
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfilePicturePath(),
                user.getRole(),
                user.getStatus()
        );
    }
}
