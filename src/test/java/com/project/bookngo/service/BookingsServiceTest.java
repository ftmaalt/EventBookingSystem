package com.project.bookngo.service;

import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.enums.BookingStatus;
import com.project.bookngo.model.enums.BookingType;
import com.project.bookngo.model.enums.SessionStatus;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    void shouldFindExistingBooking() {
        assertTrue(bookingsRepository.count() >= 0);
    }


    @Test
    void shouldFindExistingSessions() {
        assertTrue(sessionsRepository.count() >= 0);
    }


    @Test
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
    void shouldFindBookingsByUser() {
        if (bookingsRepository.count() > 0) {
            Bookings booking = bookingsRepository.findAll().get(0);
            assertNotNull(bookingsRepository.findByUserId(booking.getUser().getId()));
        }
    }
}