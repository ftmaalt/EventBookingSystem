package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Category;
import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.ActivityRequest;
import com.project.bookngo.model.response.ActivityResponse;
import com.project.bookngo.model.enums.ActivityStatus;
import com.project.bookngo.repository.ActivitiesRepository;
import com.project.bookngo.repository.CategoryRepository;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import com.project.bookngo.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ActivitiesService {
    @Autowired
    private ActivitiesRepository activitiesRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UsersRepository usersRepository;
    private static final Logger logger = LoggerFactory.getLogger(ActivitiesService.class);

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findUserByEmail(email);
    }

    public ActivityResponse createActivity(ActivityRequest request) {
        logger.info("Creating activity with title: {}", request.getTitle());        User provider = getCurrentUser();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Category with the id:" + request.getCategoryId() + " does not exist."));
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Location with the id:" + request.getLocationId() + " does not exist."));
        Activities activity = new Activities();
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setPricePerPerson(request.getPricePerPerson());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setCategory(category);
        activity.setLocation(location);
        activity.setProvider(provider);
        activity.setCreatedBy(provider.getEmail());
        activity.setUpdatedBy(provider.getEmail());
        activity.setStatus(ActivityStatus.ACTIVE);

        Activities saved = activitiesRepository.save(activity);
        logger.info("Activity created successfully with ID: {}", saved.getId());
        return toResponse(saved);
    }

    public ActivityResponse getById(Long id) {
        logger.info("Fetching activity with ID: {}", id);
        Activities activity = activitiesRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Activity with the id:" + id + " does not exist."));
        return toResponse(activity);
    }

    public List<ActivityResponse> getAllActivities() {
        logger.info("Fetching All Activities");
        return activitiesRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        logger.info("Updating activity with ID: {}", id);
        Activities activity = activitiesRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Activity with the id:" + id + " does not exist."));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InformationNotFoundException("Category with the id:" + request.getCategoryId() + " does not exist."));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new InformationNotFoundException("Location with the id:" + request.getLocationId() + " does not exist."));

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setPricePerPerson(request.getPricePerPerson());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setCategory(category);
        activity.setLocation(location);
        activity.setUpdatedBy(getCurrentUser().getEmail());
        Activities updated = activitiesRepository.save(activity);
        logger.info("Activity with ID {} updated successfully", id);
        return toResponse(updated);
    }

    public String deleteActivity(Long id) {
        logger.info("Attempting to delete activity with ID: {}", id);
        Activities activity = activitiesRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Activity with the id:" + id + " does not exist."));
        if (!activity.getProvider().getId().equals(getCurrentUser().getId())) {
            throw new InvalidCredentials("You are not authorized to modify this activity.");
        }
        activity.setUpdatedBy(getCurrentUser().getEmail());
        activitiesRepository.delete(activity);
        logger.info("Activity with ID {} deleted successfully", id);
        return "Activity with id:" + id + " has been deleted successfully.";
    }

    private ActivityResponse toResponse(Activities activities) {
        return new ActivityResponse(
                activities.getId(),
                activities.getTitle(),
                activities.getDescription(),
                activities.getPricePerPerson(),
                activities.getDurationMinutes(),
                activities.getCategory() != null ? activities.getCategory().getCategory_id(): null,
                activities.getLocation() != null ? activities.getLocation().getId() : null,
                activities.getProvider() != null ? activities.getProvider().getId() : null,
                activities.getStatus(),
                activities.getCreatedAt(),
                activities.getUpdatedAt()
        );
    }

    public Page<ActivityResponse> search(Long categoryId, Long locationId, Pageable pageable) {
        return activitiesRepository.search(categoryId, locationId, pageable).map(this::toResponse);
    }
}

