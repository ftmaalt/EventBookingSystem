package com.bookngo.catalogservice.service;

import com.bookngo.catalogservice.dto.request.ActivityRequest;
import com.bookngo.catalogservice.dto.response.ActivityResponse;
import com.bookngo.catalogservice.exception.BusinessConflictsException;
import com.bookngo.catalogservice.exception.InformationNotFoundException;
import com.bookngo.catalogservice.model.Activities;
import com.bookngo.catalogservice.model.Category;
import com.bookngo.catalogservice.model.Location;
import com.bookngo.catalogservice.model.enums.ActivityStatus;
import com.bookngo.catalogservice.repository.ActivitiesRepository;
import com.bookngo.catalogservice.repository.CategoryRepository;
import com.bookngo.catalogservice.repository.LocationRepository;
import com.bookngo.catalogservice.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ActivitiesService {

    private final ActivitiesRepository activitiesRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final CurrentUserService currentUserService;
    private static final Logger logger = LoggerFactory.getLogger(ActivitiesService.class);

    public ActivitiesService(ActivitiesRepository activitiesRepository, CategoryRepository categoryRepository, LocationRepository locationRepository, CurrentUserService currentUserService) {
        this.activitiesRepository = activitiesRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.currentUserService = currentUserService;
    }
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        logger.info("Creating activity with title: {}", request.getTitle());
        AuthenticatedUser provider = currentUserService.getCurrentUser();
        Category category = findCategory(request.getCategoryId());
        Location location = findLocation(request.getLocationId());
        assertLocationOwner(location, provider);

        Activities activity = new Activities();
        apply(activity, request, category, location);
        activity.setProviderId(provider.id());
        activity.setCreatedBy(provider.email());
        activity.setUpdatedBy(provider.email());
        activity.setStatus(ActivityStatus.ACTIVE);

        Activities saved = activitiesRepository.save(activity);
        logger.info("Activity created successfully with ID: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional
    public ActivityResponse getById(Long id) {
        Activities activity = activitiesRepository.findByIdAndStatus(id, ActivityStatus.ACTIVE)
                .orElseThrow(() -> new InformationNotFoundException(
                        "Active activity with ID " + id + " does not exist."));
        if (!Boolean.TRUE.equals(activity.getCategory().getActive())
                || !Boolean.TRUE.equals(activity.getLocation().getActive())) {
            throw new InformationNotFoundException("Activity is not available.");
        }
        return toResponse(activity);
    }

    public List<ActivityResponse> getAllActivities() {
        return activitiesRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activities activity = findActivity(id);
        AuthenticatedUser provider = currentUserService.getCurrentUser();
        assertOwner(activity, provider);

        Category category = findCategory(request.getCategoryId());
        Location location = findLocation(request.getLocationId());
        assertLocationOwner(location, provider);

        apply(activity, request, category, location);
        activity.setUpdatedBy(provider.email());
        return toResponse(activitiesRepository.save(activity));
    }

    @Transactional
    public String deleteActivity(Long id) {
        Activities activity = findActivity(id);
        AuthenticatedUser provider = currentUserService.getCurrentUser();
        assertOwner(activity, provider);
        if (activity.getStatus() == ActivityStatus.INACTIVE) {
            throw new BusinessConflictsException("Activity is already inactive.");
        }
        activity.setUpdatedBy(provider.email());
        activity.setStatus(ActivityStatus.INACTIVE);
        activitiesRepository.save(activity);
        return "Activity with id:" + id + " has been deleted successfully.";
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> search(Long categoryId, Long locationId, Pageable pageable) {
        return activitiesRepository.search(categoryId, locationId, pageable)
                .map(this::toResponse);
    }

    private void apply(Activities activities, ActivityRequest request, Category category, Location location) {
        activities.setTitle(request.getTitle());
        activities.setDescription(request.getDescription());
        activities.setPricePerPerson(request.getPricePerPerson());
        activities.setDurationMinutes(request.getDurationMinutes());
        activities.setCategory(category);
        activities.setLocation(location);
    }

    private void assertOwner(Activities activity, AuthenticatedUser user) {
        if (!Objects.equals(activity.getProviderId(), user.id())) {
            throw new AccessDeniedException("You are not authorized to modify this activity.");
        }
    }
    private void assertLocationOwner(Location location, AuthenticatedUser user) {
        if (!Objects.equals(location.getProviderId(), user.id())) {
            throw new AccessDeniedException("You are not authorized to use this location.");
        }
    }

    private Activities findActivity(Long id) {
        return activitiesRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Activity with the id:" + id + " does not exist."));
    }

    private Category findCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException(
                        "Category with ID " + id + " does not exist."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BusinessConflictsException("Cannot use an inactive category.");
        }

        return category;
    }

    private Location findLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException(
                        "Location with ID " + id + " does not exist."));

        if (!Boolean.TRUE.equals(location.getActive())) {
            throw new BusinessConflictsException("Cannot use an inactive location.");
        }
        return location;
    }

    private ActivityResponse toResponse(Activities a) {
        return new ActivityResponse(
                a.getId(), a.getTitle(), a.getDescription(), a.getPricePerPerson(), a.getDurationMinutes(),
                a.getCategory() != null ? a.getCategory().getId() : null,
                a.getLocation() != null ? a.getLocation().getId() : null,
                a.getProviderId(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt());
    }

}
