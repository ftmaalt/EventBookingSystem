package com.project.bookngo.controller;

import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.model.response.BookingResponse;
import com.project.bookngo.service.BookingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/bookings")
public class BookingsController {

    @Autowired
    private BookingsService bookingsService;

    private static final Logger logger = LoggerFactory.getLogger(BookingsController.class);
    @Operation(summary = "Create a booking", description = "Creates a booking for the authenticated customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking request"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User is not authorized"),
            @ApiResponse(responseCode = "404", description = "Session or activity not found"),
            @ApiResponse(responseCode = "409", description = "Booking conflict, such as no available seats")})
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        logger.info("Calling createBooking ===>");
        BookingResponse response = bookingsService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
        logger.info("Calling getById ===>");
        return ResponseEntity.ok(bookingsService.getById(id));
    }


    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getMyBookings(@RequestParam(required = false)BookingStatus status, @PageableDefault(size = 10,  sort = "createdAt", direction = Sort.Direction.DESC )Pageable pageable) {
        logger.info("Calling getMyBookings ===>");
        return ResponseEntity.ok(bookingsService.getMyBookings(status, pageable));
    }
    @Operation(summary = "Cancel a booking", description = "Cancels an existing booking and may allocate the released seat to a waitlisted customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to cancel this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking cannot be cancelled in its current state")})
    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        logger.info("Calling cancelBooking ===>");
        return ResponseEntity.ok(bookingsService.cancelBooking(id));
    }
}