package com.bookngo.complianceservice.controller;

import com.bookngo.complianceservice.dto.request.ReportViolationRequest;
import com.bookngo.complianceservice.dto.response.GenericMessageResponse;
import com.bookngo.complianceservice.service.ViolationsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/violations")
public class ViolationsController {

    private final ViolationsService violationsService;

    public ViolationsController(ViolationsService violationsService) {
        this.violationsService = violationsService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<GenericMessageResponse> reportViolation(@Valid @RequestBody ReportViolationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(violationsService.reportViolation(request));
    }
}
