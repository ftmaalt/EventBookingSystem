package com.project.bookngo.service;

import com.project.bookngo.exception.InformationExistsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.ProviderApplication;
import com.project.bookngo.model.ProviderProfile;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.ApplicationStatus;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.request.ProviderApplicationRequest;
import com.project.bookngo.model.request.ReviewApplicationRequest;
import com.project.bookngo.model.response.ProviderApplicationResponse;
import com.project.bookngo.repository.ProviderApplicationRepository;
import com.project.bookngo.repository.ProviderProfileRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderApplicationService {
    @Autowired
    private ProviderApplicationRepository applicationRepository;

    @Autowired
    private ProviderProfileRepository profileRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    private static final Logger logger = LoggerFactory.getLogger(ProviderApplicationService.class);

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    public ProviderApplicationResponse submitApplication(ProviderApplicationRequest applicationRequest){
        logger.info("Submitting provider application");
        User user= getCurrentUser();
        if (user.getRole() == UserRole.ADMIN || user.getRole()== UserRole.PROVIDER){
            throw new InformationExistsException("You already have provider access.");
        }
        boolean alreadyApplied = applicationRepository.findByCreatedUserId(user.getId()).stream().anyMatch(a-> a.getStatus()== ApplicationStatus.PENDING);
        if (alreadyApplied){
            throw new InformationExistsException("You have already submitted an application before.");
        }
        ProviderApplication application = new ProviderApplication();
        application.setBusinessName(applicationRequest.getBusinessName());
        application.setContactName(applicationRequest.getContactName());
        application.setEmail(user.getEmail());
        application.setPhone(applicationRequest.getPhone());
        application.setCity(applicationRequest.getCity());
        application.setDescription(applicationRequest.getDescription());
        application.setProposedActivities(applicationRequest.getProposedActivities());
        application.setStatus(ApplicationStatus.PENDING);
        application.setCreatedUser(user);
        ProviderApplication saved = applicationRepository.save(application);
        logger.info("Provider application created successfully with ID: {}", saved.getApplication_id());

        List<User> admins =
                usersRepository.findByRole(UserRole.ADMIN);

        for (User admin : admins) {

            // Create the in-app notification FIRST
            notificationService.create(
                    admin,
                    "provider-application-pending",
                    "New provider application from "
                            + saved.getBusinessName()
                            + " is waiting for review."
            );

            logger.info(
                    "In-app provider application notification created for admin: {}",
                    admin.getEmail()
            );

            // Email is secondary
            if (
                    admin.getEmail() != null &&
                            !admin.getEmail().isBlank()
            ) {
                try {
                    emailService.sendNewApplicationNotification(
                            admin.getEmail(),
                            saved
                    );
                } catch (Exception e) {
                    logger.error(
                            "Failed to email admin {} about provider application {}",
                            admin.getEmail(),
                            saved.getApplication_id(),
                            e
                    );
                }
            }
        }

        return toResponse(saved);
    }

    public List<ProviderApplicationResponse> getMyApplications() {
        logger.info("Fetching current user's provider applications");
        User user = getCurrentUser();
        return applicationRepository.findByCreatedUserId(user.getId()).stream().map(this::toResponse).toList();
    }
    public List<ProviderApplicationResponse> getAll() {
        logger.info("Fetching all provider applications");
        return applicationRepository.findAll().stream().map(this::toResponse).toList();
    }
    public ProviderApplicationResponse getById(Long id) {
        logger.info("Fetching provider application with ID: {}", id);
        ProviderApplication application = applicationRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Application with id:" + id + " does not exist."));
        return toResponse(application);
    }

    public ProviderApplicationResponse approveApplication(Long id, ReviewApplicationRequest request) {
        logger.info("Approving provider application with ID: {}", id);
        ProviderApplication application= applicationRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Application with id:" + id + " does not exist."));
        if (application.getStatus()!=ApplicationStatus.PENDING){
            throw new IllegalArgumentException("Only pending applications can be approved.");
        }
        User admin = getCurrentUser();
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewNote(request.getReviewNote());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);
        applicationRepository.save(application);

        User applicant = application.getCreatedUser();
        applicant.setRole(UserRole.PROVIDER);
        usersRepository.save(applicant);

        ProviderProfile profile = new ProviderProfile();
        profile.setBusinessName(application.getBusinessName());
        profile.setDescription(application.getDescription());
        profile.setPhone(application.getPhone());
        profile.setUser(applicant);
        profileRepository.save(profile);
        logger.info("Provider application with ID {} approved successfully", id);

        notificationService.create(
                applicant,
                "provider-application-approved",
                "Your provider application for " + application.getBusinessName() + " was approved."
        );

        return toResponse(application);
    }
    public ProviderApplicationResponse rejectApplication(Long id, ReviewApplicationRequest request) {
        logger.info("Rejecting provider application with ID: {}", id);
        ProviderApplication application= applicationRepository.findById(id).orElseThrow(() -> new InformationNotFoundException("Application with id:" + id + " does not exist."));
        if (application.getStatus()!=ApplicationStatus.PENDING){
            throw new IllegalArgumentException("Only pending applications can be approved.");
        }
        User admin = getCurrentUser();
        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewNote(request.getReviewNote());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);
        applicationRepository.save(application);
        logger.info("Provider application with ID {} rejected successfully", id);

        User applicant = application.getCreatedUser();
        String notificationMessage = "Your provider application for " + application.getBusinessName() + " was rejected.";
        if (application.getReviewNote() != null && !application.getReviewNote().isBlank()) {
            notificationMessage += " Note: " + application.getReviewNote();
        }

        notificationService.create(
                applicant,
                "provider-application-rejected",
                notificationMessage
        );

        return toResponse(application);
    }


    private ProviderApplicationResponse toResponse(ProviderApplication a) {
        return new ProviderApplicationResponse(
                a.getApplication_id(), a.getBusinessName(), a.getContactName(),
                a.getReviewNote(), a.getCity(), a.getCreatedAt(), a.getProposedActivities(), a.getStatus()
        );
    }
}