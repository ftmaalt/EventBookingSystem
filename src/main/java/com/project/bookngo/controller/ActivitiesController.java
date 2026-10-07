package com.project.bookngo.controller;

import com.project.bookngo.model.request.ActivityRequest;
import com.project.bookngo.model.response.ActivityResponse;
import com.project.bookngo.model.response.CategoryResponse;
import com.project.bookngo.service.ActivitiesService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivitiesController {

    @Autowired
    private ActivitiesService activitiesService;

    private static final Logger logger = LoggerFactory.getLogger(ActivitiesController.class);

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityRequest request) {
        logger.info("Calling createActivity ===>");
        ActivityResponse response = activitiesService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{activity_id}")
    public ResponseEntity<ActivityResponse> getById(@PathVariable Long activity_id) {
        logger.info("Calling getById ===>");
        ActivityResponse response=activitiesService.getById(activity_id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> getAllActivities(@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long locationId, @PageableDefault(size=10, sort = "id") Pageable pageable) {
        logger.info("Calling getAllActivities ===>");
        Page<ActivityResponse> response=activitiesService.search(categoryId, locationId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{activity_id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable Long activity_id, @Valid @RequestBody ActivityRequest request) {
        logger.info("Calling updateActivity ===>");
        ActivityResponse response=activitiesService.updateActivity(activity_id, request);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{activity_id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<String> deleteActivity(@PathVariable Long activity_id) {
        logger.info("Calling deleteActivity ===>");
        String message= activitiesService.deleteActivity(activity_id);
        return ResponseEntity.ok(message);
    }
}