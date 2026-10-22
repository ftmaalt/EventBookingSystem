package com.project.bookngo.controller;

import com.project.bookngo.model.response.WaitListResponse;
import com.project.bookngo.service.WaitingService;
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
        public ResponseEntity<WaitListResponse> joinWaitlist(
                @PathVariable Long sessionId) {

            WaitListResponse response = waitlistService.joinWaitlist(sessionId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/me")
        public ResponseEntity<WaitListResponse> getMyWaitlistEntry(
                @PathVariable Long sessionId) {

            return ResponseEntity.ok(
                    waitlistService.getMyWaitListEntry(sessionId)
            );
        }

        public static void main(String[] args) {
        }        @DeleteMapping("/me")
        public ResponseEntity<String> leaveWaitlist(
                @PathVariable Long sessionId) {

            return ResponseEntity.ok(
                    waitlistService.leaveWaitlist(sessionId)
            );
        }
    }

