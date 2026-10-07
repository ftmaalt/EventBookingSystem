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

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingsController {

    @Autowired
    private BookingsService bookingsService;

    private static final Logger logger = LoggerFactory.getLogger(BookingsController.class);

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
    public ResponseEntity<Page<BookingResponse>> getMyBookings(@RequestParam(required = false)BookingStatus status, @PageableDefault(size = 10)Pageable pageable) {
        logger.info("Calling getMyBookings ===>");
        return ResponseEntity.ok(bookingsService.getMyBookings(status, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        logger.info("Calling cancelBooking ===>");
        return ResponseEntity.ok(bookingsService.cancelBooking(id));
    }
}