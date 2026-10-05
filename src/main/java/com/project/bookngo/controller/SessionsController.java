package com.project.bookngo.controller;

import com.project.bookngo.model.request.SessionRequest;
import com.project.bookngo.model.response.SessionResponse;
import com.project.bookngo.service.SessionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionsController {

    @Autowired
    private SessionsService sessionsService;


    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@RequestBody SessionRequest request) {
        SessionResponse createdSession = sessionsService.createSession(request);
        return new ResponseEntity<>(createdSession, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        SessionResponse session = sessionsService.getById(id);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<SessionResponse>> getAllSessionsByActivityId(@PathVariable Long activityId) {
        List<SessionResponse> sessions = sessionsService.getAllByActivityId(activityId);
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable Long id,
            @RequestBody SessionRequest request) {
        SessionResponse updatedSession = sessionsService.updateSession(id, request);
        return ResponseEntity.ok(updatedSession);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SessionResponse> updateSessionStatus(@PathVariable Long id) {
        SessionResponse updatedSession = sessionsService.updateSessionStatus(id);
        return ResponseEntity.ok(updatedSession);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelSession(@PathVariable Long id) {
        String message = sessionsService.cancelSession(id);
        return ResponseEntity.ok(message);
    }
}