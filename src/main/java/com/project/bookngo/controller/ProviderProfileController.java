package com.project.bookngo.controller;

import com.project.bookngo.model.request.ProviderProfileRequest;
import com.project.bookngo.model.response.ProviderProfileResponse;
import com.project.bookngo.service.ProviderProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider-profile")
public class ProviderProfileController {

    @Autowired
    private ProviderProfileService providerProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ProviderProfileResponse> getMyProfile() {
        System.out.println("Calling getMyProfile==>");
        ProviderProfileResponse profileResponse = providerProfileService.getMyProfile();
        return ResponseEntity.ok(profileResponse);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ProviderProfileResponse> updateMyProfile(@Valid @RequestBody ProviderProfileRequest request) {
        System.out.println("Calling updateMyProfile==>");
        ProviderProfileResponse profileResponse = providerProfileService.updateMyProfile(request);
        return ResponseEntity.ok(profileResponse);
    }
}
