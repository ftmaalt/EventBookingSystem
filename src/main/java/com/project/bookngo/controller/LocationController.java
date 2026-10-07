package com.project.bookngo.controller;

import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.service.LocationService;
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
@RequestMapping("api/locations")
public class LocationController {

    @Autowired
    private LocationService locationService;

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        logger.info("Calling create ==>");
        LocationResponse locationResponse =locationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(locationResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getById(@PathVariable Long id) {
        logger.info("Calling getById ==>");
        LocationResponse locationResponse =locationService.getById(id);
        return ResponseEntity.status(HttpStatus.OK).body(locationResponse);
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAll() {
        logger.info("Calling getAll ==>");
        List<LocationResponse> locationResponse =locationService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(locationResponse);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<LocationResponse> update(@PathVariable Long id,@Valid @RequestBody LocationRequest request) {
        logger.info("Calling update ==>");
        LocationResponse locationResponse =locationService.update(id,request);
        return ResponseEntity.status(HttpStatus.OK).body(locationResponse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        logger.info("Calling delete ==>");
        String message= locationService.delete(id);
        return ResponseEntity.ok(message);
    }
}