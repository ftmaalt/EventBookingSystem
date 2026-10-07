package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.ProviderProfile;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.UserProfileRequest;
import com.project.bookngo.model.response.UserProfileResponse;
import com.project.bookngo.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileService {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired private FileStorageService fileStorageService;

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Fetching current user profile for email: {}", email);
        User user = usersRepository.findUserByEmail(email);
        if (user == null) {
            logger.warn("Authenticated user not found for email: {}", email);
            throw new InformationNotFoundException("Authenticated user not found.");
        }
        return user;
    }

    public UserProfileResponse getMyProfile() {
        logger.info("Fetching current user's profile");
        return toResponse(getCurrentUser());
    }

    public UserProfileResponse updateMyProfile(UserProfileRequest request) {
        logger.info("Updating current user's profile");
        User user = getCurrentUser();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        User saved = usersRepository.save(user);
        logger.info("Current user's profile updated successfully");
        return toResponse(saved);
    }

    public UserProfileResponse uploadProfilePicture(MultipartFile file) {
        logger.info("Uploading profile picture for current user");
        User user = getCurrentUser();
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