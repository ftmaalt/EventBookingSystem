package com.bookngo.bookingservice.controller;

import com.bookngo.bookingservice.dto.request.BookingRequest;
import com.bookngo.bookingservice.dto.response.BookingResponse;
import com.bookngo.bookingservice.model.enums.BookingStatus;
import com.bookngo.bookingservice.service.BookingsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingsController {

    private final BookingsService bookingsService;

    public BookingsController(BookingsService bookingsService) {
        this.bookingsService = bookingsService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingsService.createBooking(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingsService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookingsService.getMyBookings(status, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingsService.cancelBooking(id));
    }
}
