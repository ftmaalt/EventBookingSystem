package com.project.bookngo.controller;

import com.project.bookngo.model.response.WaitListResponse;
import com.project.bookngo.service.WaitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

    @RestController
    @RequestMapping("/api/sessions/{sessionId}/waitlist")
    @RequiredArgsConstructor
    public class WaitlistController {

        private final WaitingService waitlistService;
        @Operation(summary = "Join a session waitlist", description = "Adds the authenticated customer to the waitlist for a session.")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Successfully joined the waitlist"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "User is not authenticated"),
                @ApiResponse(responseCode = "403", description = "User is not authorized"),
                @ApiResponse(responseCode = "404", description = "Session not found"),
                @ApiResponse(responseCode = "409", description = "User cannot join the waitlist due to an existing booking or waitlist entry")})
        @PostMapping
        public ResponseEntity<WaitListResponse> joinWaitlist(
                @PathVariable Long sessionId) {

            WaitListResponse response = waitlistService.joinWaitlist(sessionId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        @Operation(summary = "Get my waitlist entry", description = "Retrieves the authenticated customer's waitlist entry for a session.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Waitlist entry retrieved successfully"),
                @ApiResponse(responseCode = "401", description = "User is not authenticated"),
                @ApiResponse(responseCode = "404", description = "No waitlist entry found")})
        @GetMapping("/me")
        public ResponseEntity<WaitListResponse> getMyWaitlistEntry(
                @PathVariable Long sessionId) {

            return ResponseEntity.ok(
                    waitlistService.getMyWaitListEntry(sessionId)
            );
        }
        @Operation(summary = "Leave a session waitlist", description = "Removes the authenticated customer from a session waitlist.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Successfully left the waitlist"),
                @ApiResponse(responseCode = "401", description = "User is not authenticated"),
                @ApiResponse(responseCode = "404", description = "Waitlist entry not found"),
                @ApiResponse(responseCode = "409", description = "Waitlist entry cannot be removed in its current state")})
        @DeleteMapping("/me")
        public ResponseEntity<String> leaveWaitlist(
                @PathVariable Long sessionId) {
            return ResponseEntity.ok(waitlistService.leaveWaitlist(sessionId));
        }
    }

