package com.bookngo.bookingservice.controller;

import com.bookngo.bookingservice.dto.response.WaitListResponse;
import com.bookngo.bookingservice.service.WaitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{sessionId}/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitingService waitlistService;

    @PostMapping
    public ResponseEntity<WaitListResponse> joinWaitlist(@PathVariable Long sessionId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.joinWaitlist(sessionId));
    }

    @GetMapping("/me")
    public ResponseEntity<WaitListResponse> getMyWaitlistEntry(@PathVariable Long sessionId) {
        return ResponseEntity.ok(waitlistService.getMyWaitListEntry(sessionId));
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> leaveWaitlist(@PathVariable Long sessionId) {
        return ResponseEntity.ok(waitlistService.leaveWaitlist(sessionId));
    }
}
