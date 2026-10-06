package com.project.bookngo.controller;

import com.project.bookngo.model.request.ActivityRequest;
import com.project.bookngo.model.response.ActivityResponse;
import com.project.bookngo.model.response.CategoryResponse;
import com.project.bookngo.service.ActivitiesService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityRequest request) {
        System.out.println("Calling createActivity ===>");
        ActivityResponse response = activitiesService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{activity_id}")
    public ResponseEntity<ActivityResponse> getById(@PathVariable Long activity_id) {
        System.out.println("Calling getById ===>");
        ActivityResponse response=activitiesService.getById(activity_id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getAllActivities() {
        System.out.println("Calling getAllActivities ===>");
        List<ActivityResponse> response=activitiesService.getAllActivities();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{activity_id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable Long activity_id, @Valid @RequestBody ActivityRequest request) {
        System.out.println("Calling updateActivity ===>");
        ActivityResponse response=activitiesService.updateActivity(activity_id, request);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{activity_id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<String> deleteActivity(@PathVariable Long activity_id) {
        System.out.println("Calling deleteActivity ===>");
        String message= activitiesService.deleteActivity(activity_id);
        return ResponseEntity.ok(message);
    }
}
