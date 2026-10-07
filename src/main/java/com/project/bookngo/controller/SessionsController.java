package com.project.bookngo.controller;

import com.project.bookngo.model.request.SessionRequest;
import com.project.bookngo.model.response.SessionResponse;
import com.project.bookngo.service.SessionsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionsController {

    @Autowired
    private SessionsService sessionsService;

    private static final Logger logger = LoggerFactory.getLogger(SessionsController.class);


    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        logger.info("Calling createSession ===>");
        SessionResponse createdSession = sessionsService.createSession(request);
        return new ResponseEntity<>(createdSession, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        logger.info("Calling getSessionById ===>");
        SessionResponse session = sessionsService.getById(id);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<SessionResponse>> getAllSessionsByActivityId(@PathVariable Long activityId) {
        logger.info("Calling getAllSessionsByActivityId ===>");
        List<SessionResponse> sessions = sessionsService.getAllByActivityId(activityId);
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> updateSession(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        logger.info("Calling updateSession ===>");
        SessionResponse updatedSession = sessionsService.updateSession(id, request);
        return ResponseEntity.ok(updatedSession);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SessionResponse> updateSessionStatus(@PathVariable Long id) {
        logger.info("Calling updateSessionStatus ===>");
        SessionResponse updatedSession = sessionsService.updateSessionStatus(id);
        return ResponseEntity.ok(updatedSession);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelSession(@PathVariable Long id) {
        logger.info("Calling cancelSession ===>");
        String message = sessionsService.cancelSession(id);
        return ResponseEntity.ok(message);
    }
}