package com.project.bookngo.controller;

import com.project.bookngo.model.request.ProviderApplicationRequest;
import com.project.bookngo.model.request.ReviewApplicationRequest;
import com.project.bookngo.model.response.ActivityResponse;
import com.project.bookngo.model.response.ProviderApplicationResponse;
import com.project.bookngo.service.ProviderApplicationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ProviderApplicationController {

    @Autowired
    private ProviderApplicationService applicationService;

    private static final Logger logger = LoggerFactory.getLogger(ProviderApplicationController.class);

    @PostMapping
    public ResponseEntity<ProviderApplicationResponse> submitApplication(@Valid @RequestBody ProviderApplicationRequest applicationRequest){
        logger.info("Calling submitApplication==>");
        ProviderApplicationResponse applicationResponse = applicationService.submitApplication(applicationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationResponse);
    }
    // check the path
    @GetMapping("/mine")
    public ResponseEntity<List<ProviderApplicationResponse>> getMyApplications(){
        logger.info("Calling getMyApplications==>");
        List<ProviderApplicationResponse> applicationResponse = applicationService.getMyApplications();
        return ResponseEntity.ok(applicationResponse);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProviderApplicationResponse>> getAll(){
        logger.info("Calling getAll==>");
        List<ProviderApplicationResponse> applicationResponse = applicationService.getAll();
        return ResponseEntity.ok(applicationResponse);
    }

    @GetMapping("/{application_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProviderApplicationResponse> getById(@PathVariable Long application_id){
        logger.info("Calling getById==>");
        ProviderApplicationResponse applicationResponse = applicationService.getById(application_id);
        return ResponseEntity.ok().body(applicationResponse);
    }

    @PatchMapping(value = "/{application_id}/status", params = "action=approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProviderApplicationResponse> approveApplication(@PathVariable Long application_id, @Valid @RequestBody ReviewApplicationRequest request) {
        logger.info("Calling approveApplication==>");
        ProviderApplicationResponse applicationResponse = applicationService.approveApplication(application_id, request);
        return ResponseEntity.ok().body(applicationResponse);
    }

    @PatchMapping(value = "/{application_id}/status", params = "action=reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProviderApplicationResponse> rejectApplication(@PathVariable Long application_id, @Valid @RequestBody ReviewApplicationRequest request) {
        logger.info("Calling rejectApplication==>");
        ProviderApplicationResponse applicationResponse = applicationService.rejectApplication(application_id, request);
        return ResponseEntity.ok().body(applicationResponse);
    }

}