package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.exception.InvalidTimeSpecificationException;
import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Bookings;
import com.project.bookngo.model.Sessions;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.model.response.BookingResponse;
import com.project.bookngo.repository.BookingsRepository;
import com.project.bookngo.repository.SessionsRepository;

import com.project.bookngo.repository.WaitingListRepository;
import com.project.bookngo.service.event.BookingNotificationEvent;
import com.project.bookngo.service.event.BookingNotificationType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EventListener;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingsServiceTest {

    @Mock
    private BookingsRepository bookingsRepository;

    @Mock
    private SessionsRepository sessionsRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private WaitingService waitingService;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingsService bookingsService;

    private User customer;
    private Sessions session;
    private BookingRequest request;

    @BeforeEach
    void setUp() {

        customer = new User();
        customer.setId(1L);
        customer.setFullName("Sara Mohamed");
        customer.setEmail("customer@test.com");
        customer.setRole(UserRole.USER);
        customer.setStatus(UserStatus.ACTIVE);

        Activities activity = new Activities();
        activity.setId(10L);
        activity.setTitle("Kayaking");
        activity.setPricePerPerson(
                new BigDecimal("15.000")
        );
        activity.setStatus(ActivityStatus.ACTIVE);

        session = new Sessions();
        session.setId(100L);
        session.setActivity(activity);
        session.setCapacity(5);
        session.setSpotsLeft(5);
        session.setStatus(SessionStatus.SCHEDULED);
        session.setStartTime(
                LocalDateTime.now().plusDays(3)
        );
        session.setEndTime(
                LocalDateTime.now().plusDays(3).plusHours(2)
        );

        request = new BookingRequest();
        request.setSessionId(session.getId());
        request.setParticipants(1);
        request.setBookingType(BookingType.INDIVIDUAL);
    }

    private void mockBookingContext() {
        when(currentUserService.getCurrentUser())
                .thenReturn(customer);

        when(sessionsRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(session));
    }

    @Test
    @DisplayName("Should reject booking for cancelled session")
    void shouldNotAllowBookingForCancelledSession() {

        session.setStatus(SessionStatus.CANCELLED);
        mockBookingContext();

        assertThrows(
                InvalidTimeSpecificationException.class,
                () -> bookingsService.createBooking(request)
        );

        verify(bookingsRepository, never())
                .save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should reject booking when session is full")
    void shouldNotAllowBookingWhenSessionIsFull() {

        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);
        mockBookingContext();

        assertThrows(
                BusinessConflictsException.class,
                () -> bookingsService.createBooking(request)
        );

        verify(bookingsRepository, never())
                .save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should reject duplicate booking")
    void shouldNotAllowDoubleBooking() {

        mockBookingContext();

        Bookings existingBooking = new Bookings();
        existingBooking.setBookingId(200L);
        existingBooking.setUser(customer);
        existingBooking.setSession(session);
        existingBooking.setStatus(BookingStatus.CONFIRMED);

        when(bookingsRepository
                .findByUserIdAndSessionIdAndStatus(
                        1L,
                        100L,
                        BookingStatus.CONFIRMED
                ))
                .thenReturn(Optional.of(existingBooking));

        assertThrows(
                BusinessConflictsException.class,
                () -> bookingsService.createBooking(request)
        );

        verify(bookingsRepository, never())
                .save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should create booking and decrease available spots")
    void shouldCreateBookingSuccessfully() {

        mockBookingContext();

        when(bookingsRepository.save(any(Bookings.class)))
                .thenAnswer(invocation -> {
                    Bookings booking = invocation.getArgument(0);
                    booking.setBookingId(200L);
                    return booking;
                });

        BookingResponse response =
                bookingsService.createBooking(request);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(BookingStatus.CONFIRMED,
                response.getStatus());

        assertEquals(4, session.getSpotsLeft());

        verify(sessionsRepository).save(session);
        verify(bookingsRepository).save(any(Bookings.class));

        verify(eventPublisher).publishEvent(new BookingNotificationEvent(200L, BookingNotificationType.BOOKING_CONFIRMED));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should return 404 exception for missing session")
    void shouldRejectNonexistentSession() {

        when(currentUserService.getCurrentUser())
                .thenReturn(customer);

        when(sessionsRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> bookingsService.createBooking(request)
        );

        verify(bookingsRepository, never())
                .save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should retrieve bookings belonging to current user")
    void shouldFindBookingsByUser() {

        Bookings booking = new Bookings();
        booking.setBookingId(200L);
        booking.setUser(customer);
        booking.setSession(session);
        booking.setParticipants(1);
        booking.setBookingType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(
                PaymentStatus.PENDING_PAYMENT
        );
        booking.setSubtotal(
                new BigDecimal("15.000")
        );
        booking.setTotalPrice(
                new BigDecimal("15.000")
        );

        PageRequest pageable = PageRequest.of(0, 10);

        when(currentUserService.getCurrentUser())
                .thenReturn(customer);

        when(bookingsRepository.findMyBookings(
                1L,
                null,
                pageable
        )).thenReturn(
                new PageImpl<>(List.of(booking))
        );

        Page<BookingResponse> result =
                bookingsService.getMyBookings(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(200L, result.getContent().get(0).getId());

        verify(bookingsRepository).findMyBookings(
                1L,
                null,
                pageable
        );
    }

    @Test
    @DisplayName("Cancellation releases seats and invokes waitlist allocation")
    void shouldCancelBookingAndAllocateWaitlist() {

        Bookings booking = new Bookings();
        booking.setBookingId(200L);
        booking.setUser(customer);
        booking.setSession(session);
        booking.setParticipants(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        session.setCapacity(5);
        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);

        when(bookingsRepository.findById(200L))
                .thenReturn(Optional.of(booking));

        when(currentUserService.getCurrentUser())
                .thenReturn(customer);

        when(sessionsRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(session));

        String result = bookingsService.cancelBooking(200L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(1, session.getSpotsLeft());
        assertEquals(SessionStatus.SCHEDULED, session.getStatus());

        assertTrue(result.contains("cancelled successfully"));

        verify(entityManager).refresh(booking);
        verify(waitingService).allocateNextWaitlistedUser(session);
        verify(bookingsRepository).save(booking);
        verify(sessionsRepository).save(session);

        verify(eventPublisher).publishEvent(new BookingNotificationEvent(200L, BookingNotificationType.BOOKING_CANCELLED));

    }


    @Test
    @DisplayName("Cannot cancel a booking twice")
    void shouldRejectAlreadyCancelledBooking() {

        Bookings booking = new Bookings();
        booking.setBookingId(200L);
        booking.setUser(customer);
        booking.setSession(session);
        booking.setParticipants(1);
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingsRepository.findById(200L))
                .thenReturn(Optional.of(booking));

        when(currentUserService.getCurrentUser())
                .thenReturn(customer);

        when(sessionsRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(session));

        assertThrows(
                BusinessConflictsException.class,
                () -> bookingsService.cancelBooking(200L)
        );

        verify(waitingService, never())
                .allocateNextWaitlistedUser(any(Sessions.class));

        verify(bookingsRepository, never())
                .save(any(Bookings.class));

        verify(sessionsRepository, never())
                .save(any(Sessions.class));
    }

    @Test
    @DisplayName("Cannot cancel a booking after session start")
    void shouldRejectCancellationAfterSessionStart() {

        Bookings booking = new Bookings();
        booking.setBookingId(200L);
        booking.setUser(customer);
        booking.setSession(session);
        booking.setParticipants(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        session.setStartTime(LocalDateTime.now().minusHours(1));

        when(bookingsRepository.findById(200L))
                .thenReturn(Optional.of(booking));

        when(currentUserService.getCurrentUser())
                .thenReturn(customer);

        when(sessionsRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(session));

        assertThrows(
                BusinessConflictsException.class,
                () -> bookingsService.cancelBooking(200L)
        );

        verify(waitingService, never())
                .allocateNextWaitlistedUser(any(Sessions.class));

        verify(bookingsRepository, never())
                .save(any(Bookings.class));
    }
    @Test
    @DisplayName("Should prevent direct booking when customers are waiting")
    void shouldRejectDirectBookingWhenWaitlistExists() {

        mockBookingContext();

        when(waitingListRepository.existsBySessionIdAndStatus(
                100L,
                WaitListStatus.WAITING
        )).thenReturn(true);

        assertThrows(
                BusinessConflictsException.class,
                () -> bookingsService.createBooking(request)
        );

        verify(bookingsRepository, never())
                .save(any(Bookings.class));

        verify(sessionsRepository, never())
                .save(any(Sessions.class));
    }



}