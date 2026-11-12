package com.bookngo.bookingservice.controller;

import com.bookngo.bookingservice.dto.request.SessionRequest;
import com.bookngo.bookingservice.dto.response.SessionResponse;
import com.bookngo.bookingservice.service.SessionsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionsController {

    private final SessionsService sessionsService;

    public SessionsController(SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionsService.createSession(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionsService.getById(id));
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<SessionResponse>> getAllSessionsByActivityId(@PathVariable Long activityId) {
        return ResponseEntity.ok(sessionsService.getAllByActivityId(activityId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<SessionResponse> updateSession(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(sessionsService.updateSession(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<SessionResponse> updateSessionStatus(@PathVariable Long id) {
        return ResponseEntity.ok(sessionsService.updateSessionStatus(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<String> cancelSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionsService.cancelSession(id));
    }
}
