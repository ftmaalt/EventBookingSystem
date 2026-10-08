package com.project.bookngo.service;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.enums.BookingType;
import com.project.bookngo.model.enums.SessionStatus;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingsServiceTest {

    @Autowired
    private BookingsService bookingsService;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private SessionsRepository sessionsRepository;

    @Test
    @DisplayName("Should not allow booking for a cancelled session")
    void shouldNotAllowBookingForCancelledSession() {
        Sessions session = sessionsRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.CANCELLED)
                .findFirst()
                .orElse(null);

        if (session != null) {
            BookingRequest request = new BookingRequest();
            request.setSessionId(session.getId());
            request.setParticipants(1);
            request.setBookingType(BookingType.INDIVIDUAL);

            assertThrows(Exception.class, () -> bookingsService.createBooking(request));
        }
    }


    @Test
    @DisplayName("Should not allow booking when a session is full")
    void shouldNotAllowBookingWhenSessionIsFull() {
        Sessions session = sessionsRepository.findAll().stream()
                .filter(s -> s.getSpotsLeft() == 0)
                .findFirst()
                .orElse(null);

        if (session != null) {
            BookingRequest request = new BookingRequest();
            request.setSessionId(session.getId());
            request.setParticipants(1);
            request.setBookingType(BookingType.INDIVIDUAL);

            assertThrows(Exception.class, () -> bookingsService.createBooking(request));
        }
    }
    @Test
    @DisplayName("Should not allow a user to book the same session twice")
    void shouldNotAllowDoubleBooking() {

        Bookings existingBooking = bookingsRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("No confirmed booking exists in the test database."));

        authenticateAs(existingBooking.getUser().getEmail());

        BookingRequest request = new BookingRequest();
        request.setSessionId(existingBooking.getSession().getId());
        request.setParticipants(1);
        request.setBookingType(BookingType.INDIVIDUAL);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingsService.createBooking(request));
    }


    @Test
    @DisplayName("Should find bookings for an existing user")
    void shouldFindBookingsByUser() {
        if (bookingsRepository.count() > 0) {
            Bookings booking = bookingsRepository.findAll().get(0);
            assertNotNull(bookingsRepository.findByUserId(booking.getUser().getId()));
        }
    }

    private void authenticateAs(String email) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken( email, null);
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}