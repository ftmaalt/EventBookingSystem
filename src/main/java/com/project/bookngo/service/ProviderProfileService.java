package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.ProviderProfile;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.ProviderProfileRequest;
import com.project.bookngo.model.response.ProviderApplicationResponse;
import com.project.bookngo.model.response.ProviderProfileResponse;
import com.project.bookngo.repository.ProviderProfileRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ProviderProfileService {

    @Autowired
    private ProviderProfileRepository providerProfileRepository;
    @Autowired
    private UsersRepository usersRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    public ProviderProfileResponse getMyProfile() {
        System.out.println("SERVICE Calling getMyProfile==>");
        User user= getCurrentUser();
        ProviderProfile profile= providerProfileRepository.findByUserId(user.getId()).orElseThrow(()-> new InformationNotFoundException("Provider Profile Doesn't exist for this user."));
        return toResponse(profile);
    }

    public ProviderProfileResponse updateMyProfile(ProviderProfileRequest request) {
        System.out.println("SERVICE Calling updateMyProfile==>");
        User user= getCurrentUser();
        ProviderProfile profile= providerProfileRepository.findByUserId(user.getId()).orElseThrow(()-> new InformationNotFoundException("Provider Profile Doesn't exist for this user."));
        profile.setDescription(request.getDescription());
        profile.setPhone(request.getPhone());
        providerProfileRepository.save(profile);

        return toResponse(profile);
    }

    private ProviderProfileResponse toResponse(ProviderProfile profile) {
        return new ProviderProfileResponse(
                profile.getProvider_id(), profile.getBusinessName(), profile.getPhone(), profile.getDescription(), profile.getUpdatedAt(), profile.getCreatedAt()
        );
}
}
