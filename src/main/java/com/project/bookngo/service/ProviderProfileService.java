package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.ProviderProfile;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.ProviderProfileRequest;
import com.project.bookngo.model.response.ProviderProfileResponse;
import com.project.bookngo.repository.ProviderProfileRepository;
import com.project.bookngo.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProviderProfileService {

    @Autowired
    private ProviderProfileRepository providerProfileRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private CurrentUserService currentUserService;

    private static final Logger logger = LoggerFactory.getLogger(ProviderProfileService.class);


    public ProviderProfileResponse getMyProfile() {
        logger.info("Fetching current provider profile");
        User user = currentUserService.getCurrentUser();
        ProviderProfile profile= providerProfileRepository.findByUserId(user.getId()).orElseThrow(()-> new InformationNotFoundException("Provider Profile Doesn't exist for this user."));
        return toResponse(profile);
    }

    public ProviderProfileResponse updateMyProfile(ProviderProfileRequest request) {
        logger.info("Updating current provider profile");
        User user = currentUserService.getCurrentUser();
        ProviderProfile profile= providerProfileRepository.findByUserId(user.getId()).orElseThrow(()-> new InformationNotFoundException("Provider Profile Doesn't exist for this user."));
        profile.setDescription(request.getDescription());
        profile.setPhone(request.getPhone());
        providerProfileRepository.save(profile);
        logger.info("Provider profile updated successfully for user ID: {}", user.getId());

        return toResponse(profile);
    }

    private ProviderProfileResponse toResponse(ProviderProfile profile) {
        return new ProviderProfileResponse(
                profile.getProviderId(), profile.getBusinessName(), profile.getPhone(), profile.getDescription(), profile.getUpdatedAt(), profile.getCreatedAt()
        );
    }
}