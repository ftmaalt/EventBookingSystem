package com.project.bookngo.controller;

import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.service.LocationService;
import jakarta.validation.Valid;
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

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        System.out.println("Calling create ==>");
        LocationResponse locationResponse =locationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(locationResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getById(@PathVariable Long id) {
        System.out.println("Calling getById ==>");
        LocationResponse locationResponse =locationService.getById(id);
        return ResponseEntity.status(HttpStatus.OK).body(locationResponse);
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAll() {
        System.out.println("Calling getAll ==>");
        List<LocationResponse> locationResponse =locationService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(locationResponse);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<LocationResponse> update(@PathVariable Long id,@Valid @RequestBody LocationRequest request) {
        System.out.println("Calling update ==>");
        LocationResponse locationResponse =locationService.update(id,request);
        return ResponseEntity.status(HttpStatus.OK).body(locationResponse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        System.out.println("Calling delete ==>");
        String message= locationService.delete(id);
        return ResponseEntity.ok(message);
    }
}
