package com.bookngo.bookingservice;

import com.bookngo.bookingservice.dto.request.BookingRequest;
import com.bookngo.bookingservice.dto.response.BookingResponse;
import com.bookngo.bookingservice.event.BookingEvent;
import com.bookngo.bookingservice.event.BookingEventType;
import com.bookngo.bookingservice.exception.BusinessConflictsException;
import com.bookngo.bookingservice.exception.InformationNotFoundException;
import com.bookngo.bookingservice.exception.InvalidTimeSpecificationException;
import com.bookngo.bookingservice.model.Bookings;
import com.bookngo.bookingservice.model.Sessions;
import com.bookngo.bookingservice.model.enums.BookingStatus;
import com.bookngo.bookingservice.model.enums.BookingType;
import com.bookngo.bookingservice.model.enums.SessionStatus;
import com.bookngo.bookingservice.model.enums.WaitListStatus;
import com.bookngo.bookingservice.repository.BookingsRepository;
import com.bookngo.bookingservice.repository.SessionsRepository;
import com.bookngo.bookingservice.repository.WaitingListRepository;
import com.bookngo.bookingservice.security.AuthenticatedUser;
import com.bookngo.bookingservice.service.BookingsService;
import com.bookngo.bookingservice.service.CurrentUserService;
import com.bookngo.bookingservice.service.WaitingService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private EntityManager entityManager;

    @Mock
    private WaitingService waitingService;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingsService bookingsService;

    private AuthenticatedUser customer;
    private Sessions session;
    private BookingRequest request;

    @BeforeEach
    void setUp() {
        customer = new AuthenticatedUser(1L, "customer@test.com", "USER");

        session = new Sessions();
        session.setId(100L);
        session.setActivityId(10L);
        session.setActivityTitle("Kayaking");
        session.setProviderId(50L);
        session.setPricePerPerson(new BigDecimal("15.000"));
        session.setCapacity(5);
        session.setSpotsLeft(5);
        session.setStatus(SessionStatus.SCHEDULED);
        session.setStartTime(LocalDateTime.now().plusDays(3));
        session.setEndTime(LocalDateTime.now().plusDays(3).plusHours(2));

        request = new BookingRequest();
        request.setSessionId(session.getId());
        request.setParticipants(1);
        request.setBookingType(BookingType.INDIVIDUAL);
    }

    private void mockBookingContext() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(sessionsRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));
    }

    private Bookings booking(BookingStatus status) {
        Bookings booking = new Bookings();
        booking.setBookingId(200L);
        booking.setUserId(customer.id());
        booking.setUserEmail(customer.email());
        booking.setSession(session);
        booking.setParticipants(1);
        booking.setBookingType(BookingType.INDIVIDUAL);
        booking.setSubtotal(new BigDecimal("15.000"));
        booking.setTotalPrice(new BigDecimal("15.000"));
        booking.setStatus(status);
        return booking;
    }

    private BookingEvent publishedEvent() {
        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Should reject booking for cancelled session")
    void shouldNotAllowBookingForCancelledSession() {
        session.setStatus(SessionStatus.CANCELLED);
        mockBookingContext();

        assertThrows(InvalidTimeSpecificationException.class, () -> bookingsService.createBooking(request));

        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should reject booking when session is full")
    void shouldNotAllowBookingWhenSessionIsFull() {
        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);
        mockBookingContext();

        assertThrows(BusinessConflictsException.class, () -> bookingsService.createBooking(request));

        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should reject booking for more participants than seats left")
    void shouldNotAllowMoreParticipantsThanSeatsLeft() {
        session.setSpotsLeft(2);
        request.setParticipants(3);
        mockBookingContext();

        assertThrows(BusinessConflictsException.class, () -> bookingsService.createBooking(request));

        assertEquals(2, session.getSpotsLeft());
        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should reject duplicate booking")
    void shouldNotAllowDoubleBooking() {
        mockBookingContext();
        when(bookingsRepository.existsByUserIdAndSession_IdAndStatusIn(eq(1L), eq(100L), anyCollection()))
                .thenReturn(true);

        assertThrows(BusinessConflictsException.class, () -> bookingsService.createBooking(request));

        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should create booking and decrease available spots")
    void shouldCreateBookingSuccessfully() {
        mockBookingContext();
        when(bookingsRepository.save(any(Bookings.class))).thenAnswer(invocation -> {
            Bookings booking = invocation.getArgument(0);
            booking.setBookingId(200L);
            return booking;
        });

        BookingResponse response = bookingsService.createBooking(request);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertEquals(new BigDecimal("15.000"), response.getTotalPrice());
        assertEquals(4, session.getSpotsLeft());

        verify(sessionsRepository).save(session);
        verify(bookingsRepository).save(any(Bookings.class));

        BookingEvent event = publishedEvent();
        assertEquals(BookingEventType.BOOKING_CONFIRMED, event.eventType());
        assertEquals(200L, event.bookingId());
        assertEquals(1L, event.userId());
        assertEquals("Kayaking", event.activityTitle());
    }

    @Test
    @DisplayName("Should mark session full when the last seat is booked")
    void shouldMarkSessionFullWhenLastSeatIsBooked() {
        session.setSpotsLeft(1);
        mockBookingContext();
        when(bookingsRepository.save(any(Bookings.class))).thenAnswer(invocation -> {
            Bookings booking = invocation.getArgument(0);
            booking.setBookingId(201L);
            return booking;
        });

        bookingsService.createBooking(request);

        assertEquals(0, session.getSpotsLeft());
        assertEquals(SessionStatus.FULL, session.getStatus());
    }

    @Test
    @DisplayName("Should return 404 exception for missing session")
    void shouldRejectNonexistentSession() {
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(sessionsRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(InformationNotFoundException.class, () -> bookingsService.createBooking(request));

        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should retrieve bookings belonging to current user")
    void shouldFindBookingsByUser() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(bookingsRepository.findMyBookings(1L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(booking(BookingStatus.CONFIRMED))));

        Page<BookingResponse> result = bookingsService.getMyBookings(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(200L, result.getContent().get(0).getId());
        verify(bookingsRepository).findMyBookings(1L, null, pageable);
    }

    @Test
    @DisplayName("Cancellation releases seats and invokes waitlist allocation")
    void shouldCancelBookingAndAllocateWaitlist() {
        Bookings booking = booking(BookingStatus.CONFIRMED);
        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);

        when(bookingsRepository.findById(200L)).thenReturn(Optional.of(booking));
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(sessionsRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));

        String result = bookingsService.cancelBooking(200L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(1, session.getSpotsLeft());
        assertEquals(SessionStatus.SCHEDULED, session.getStatus());
        assertTrue(result.contains("cancelled successfully"));

        verify(entityManager).refresh(booking);
        verify(waitingService).allocateNextWaitlistedUser(session);
        verify(bookingsRepository).save(booking);
        verify(sessionsRepository).save(session);

        BookingEvent event = publishedEvent();
        assertEquals(BookingEventType.BOOKING_CANCELLED, event.eventType());
        assertEquals(200L, event.bookingId());
    }

    @Test
    @DisplayName("Cannot cancel a booking twice")
    void shouldRejectAlreadyCancelledBooking() {
        when(bookingsRepository.findById(200L)).thenReturn(Optional.of(booking(BookingStatus.CANCELLED)));
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(sessionsRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));

        assertThrows(BusinessConflictsException.class, () -> bookingsService.cancelBooking(200L));

        verify(waitingService, never()).allocateNextWaitlistedUser(any(Sessions.class));
        verify(bookingsRepository, never()).save(any(Bookings.class));
        verify(sessionsRepository, never()).save(any(Sessions.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Cannot cancel a booking after session start")
    void shouldRejectCancellationAfterSessionStart() {
        session.setStartTime(LocalDateTime.now().minusHours(1));
        when(bookingsRepository.findById(200L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));
        when(currentUserService.getCurrentUser()).thenReturn(customer);
        when(sessionsRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));

        assertThrows(BusinessConflictsException.class, () -> bookingsService.cancelBooking(200L));

        verify(waitingService, never()).allocateNextWaitlistedUser(any(Sessions.class));
        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Cannot cancel another user's booking")
    void shouldRejectCancellationByAnotherUser() {
        when(bookingsRepository.findById(200L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));
        when(currentUserService.getCurrentUser()).thenReturn(new AuthenticatedUser(2L, "other@test.com", "USER"));
        when(sessionsRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> bookingsService.cancelBooking(200L));

        verify(bookingsRepository, never()).save(any(Bookings.class));
    }

    @Test
    @DisplayName("Should prevent direct booking when customers are waiting")
    void shouldRejectDirectBookingWhenWaitlistExists() {
        mockBookingContext();
        when(waitingListRepository.existsBySessionIdAndStatus(100L, WaitListStatus.WAITING)).thenReturn(true);

        assertThrows(BusinessConflictsException.class, () -> bookingsService.createBooking(request));

        verify(bookingsRepository, never()).save(any(Bookings.class));
        verify(sessionsRepository, never()).save(any(Sessions.class));
    }
}
