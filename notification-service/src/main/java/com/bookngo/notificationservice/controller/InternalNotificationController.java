package com.bookngo.notificationservice.controller;

import com.bookngo.notificationservice.dto.request.NotificationEventRequest;
import com.bookngo.notificationservice.dto.response.GenericMessageResponse;
import com.bookngo.notificationservice.service.NotificationDispatcher;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** For other BookNGo services only. */
@RestController
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController {

    private final NotificationDispatcher dispatcher;

    @Value("${app.internal-api-key:}")
    private String internalApiKey;

    public InternalNotificationController(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping
    public ResponseEntity<GenericMessageResponse> send(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody NotificationEventRequest request) {
        if (!isValidKey(key)) {
            throw new AccessDeniedException("Invalid internal key.");
        }
        dispatcher.dispatch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenericMessageResponse("Notification accepted."));
    }

    private boolean isValidKey(String key) {
        if (internalApiKey == null || internalApiKey.isBlank() || key == null) {
            return false;
        }
        return MessageDigest.isEqual(internalApiKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }
}
