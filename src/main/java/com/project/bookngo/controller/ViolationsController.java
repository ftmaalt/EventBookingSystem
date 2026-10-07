package com.project.bookngo.controller;

import com.project.bookngo.model.request.ReportViolationRequest;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.service.ViolationsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/violations")
public class ViolationsController {
    @Autowired
    private ViolationsService violationsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<GenericMessageResponse> reportViolation(@Valid @RequestBody ReportViolationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(violationsService.reportViolation(request));
    }
}
