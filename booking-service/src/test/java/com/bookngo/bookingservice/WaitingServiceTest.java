package com.bookngo.bookingservice;

import com.bookngo.bookingservice.event.BookingEvent;
import com.bookngo.bookingservice.event.BookingEventType;
import com.bookngo.bookingservice.model.Bookings;
import com.bookngo.bookingservice.model.Sessions;
import com.bookngo.bookingservice.model.WaitlistEntry;
import com.bookngo.bookingservice.model.enums.BookingStatus;
import com.bookngo.bookingservice.model.enums.BookingType;
import com.bookngo.bookingservice.model.enums.SessionStatus;
import com.bookngo.bookingservice.model.enums.WaitListStatus;
import com.bookngo.bookingservice.repository.BookingsRepository;
import com.bookngo.bookingservice.repository.SessionsRepository;
import com.bookngo.bookingservice.repository.WaitingListRepository;
import com.bookngo.bookingservice.service.CurrentUserService;
import com.bookngo.bookingservice.service.WaitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingServiceTest {

    @Mock
    private WaitingListRepository waitlistRepository;

    @Mock
    private SessionsRepository sessionsRepository;

    @Mock
    private BookingsRepository bookingsRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WaitingService waitingService;

    private Sessions session;
    private WaitlistEntry firstEntry;
    private WaitlistEntry secondEntry;

    @BeforeEach
    void setUp() {
        session = new Sessions();
        session.setId(100L);
        session.setActivityId(10L);
        session.setActivityTitle("Kayaking");
        session.setPricePerPerson(new BigDecimal("15.000"));
        session.setCapacity(5);
        session.setSpotsLeft(1);
        session.setStatus(SessionStatus.SCHEDULED);
        session.setStartTime(LocalDateTime.now().plusDays(3));

        firstEntry = entry(10L, 1L, "first@test.com");
        secondEntry = entry(11L, 2L, "second@test.com");
    }

    private WaitlistEntry entry(Long entryId, Long userId, String email) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setEntryId(entryId);
        entry.setUserId(userId);
        entry.setUserEmail(email);
        entry.setSession(session);
        entry.setStatus(WaitListStatus.WAITING);
        return entry;
    }

    private void mockBookingSave() {
        when(bookingsRepository.save(any(Bookings.class))).thenAnswer(invocation -> {
            Bookings booking = invocation.getArgument(0);
            booking.setBookingId(500L);
            return booking;
        });
    }

    private void mockWaitingEntries() {
        when(waitlistRepository.findBySessionIdAndStatusOrderByEntryIdAsc(100L, WaitListStatus.WAITING))
                .thenReturn(List.of(firstEntry, secondEntry));
    }

    @Test
    void shouldAllocateFirstWaitingCustomer() {
        mockWaitingEntries();
        mockBookingSave();

        waitingService.allocateNextWaitlistedUser(session);

        assertEquals(WaitListStatus.BOOKED, firstEntry.getStatus());
        assertEquals(WaitListStatus.WAITING, secondEntry.getStatus());
        assertEquals(0, session.getSpotsLeft());
        assertEquals(SessionStatus.FULL, session.getStatus());

        ArgumentCaptor<Bookings> captor = ArgumentCaptor.forClass(Bookings.class);
        verify(bookingsRepository, times(1)).save(captor.capture());
        Bookings savedBooking = captor.getValue();
        assertEquals(1L, savedBooking.getUserId());
        assertEquals("first@test.com", savedBooking.getUserEmail());
        assertEquals(1, savedBooking.getParticipants());
        assertEquals(BookingType.INDIVIDUAL, savedBooking.getBookingType());
        assertEquals(BookingStatus.CONFIRMED, savedBooking.getStatus());
        assertEquals(0, new BigDecimal("15.000").compareTo(savedBooking.getTotalPrice()));

        verify(waitlistRepository).save(firstEntry);
        verify(waitlistRepository, never()).save(secondEntry);

        ArgumentCaptor<BookingEvent> event = ArgumentCaptor.forClass(BookingEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(BookingEventType.WAITLIST_ALLOCATED, event.getValue().eventType());
        assertEquals(500L, event.getValue().bookingId());
        assertEquals(1L, event.getValue().userId());
    }

    @Test
    void shouldAllocateSeveralCustomersWhenSeveralSeatsAreFree() {
        session.setSpotsLeft(2);
        mockWaitingEntries();
        mockBookingSave();

        waitingService.allocateNextWaitlistedUser(session);

        assertEquals(WaitListStatus.BOOKED, firstEntry.getStatus());
        assertEquals(WaitListStatus.BOOKED, secondEntry.getStatus());
        assertEquals(0, session.getSpotsLeft());
        verify(bookingsRepository, times(2)).save(any(Bookings.class));
        verify(eventPublisher, times(2)).publishEvent(any(BookingEvent.class));
    }

    @Test
    void shouldNotAllocateWithoutAvailableSeats() {
        session.setSpotsLeft(0);

        waitingService.allocateNextWaitlistedUser(session);

        verifyNoInteractions(bookingsRepository);
        verifyNoInteractions(waitlistRepository);
    }

    @Test
    void shouldExpireCustomerWhoAlreadyHasAnActiveBooking() {
        mockWaitingEntries();
        mockBookingSave();
        when(bookingsRepository.existsByUserIdAndSession_IdAndStatus(1L, 100L, BookingStatus.CONFIRMED))
                .thenReturn(true);

        waitingService.allocateNextWaitlistedUser(session);

        assertEquals(WaitListStatus.EXPIRED, firstEntry.getStatus());
        assertEquals(WaitListStatus.BOOKED, secondEntry.getStatus());
        verify(waitlistRepository).save(firstEntry);
        verify(waitlistRepository).save(secondEntry);

        ArgumentCaptor<Bookings> captor = ArgumentCaptor.forClass(Bookings.class);
        verify(bookingsRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getUserId());
        assertEquals(0, session.getSpotsLeft());
    }

    @Test
    void shouldExpireAllEntriesWhenSessionHasAlreadyStarted() {
        session.setStartTime(LocalDateTime.now().minusMinutes(1));
        mockWaitingEntries();

        waitingService.allocateNextWaitlistedUser(session);

        assertEquals(WaitListStatus.EXPIRED, firstEntry.getStatus());
        assertEquals(WaitListStatus.EXPIRED, secondEntry.getStatus());
        verify(waitlistRepository).saveAll(List.of(firstEntry, secondEntry));
        verifyNoInteractions(bookingsRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldExpireAllEntriesWhenSessionIsCancelled() {
        session.setStatus(SessionStatus.CANCELLED);
        mockWaitingEntries();

        waitingService.allocateNextWaitlistedUser(session);

        assertEquals(WaitListStatus.EXPIRED, firstEntry.getStatus());
        assertEquals(WaitListStatus.EXPIRED, secondEntry.getStatus());
        verify(waitlistRepository).saveAll(List.of(firstEntry, secondEntry));
        verifyNoInteractions(bookingsRepository);
        verifyNoInteractions(eventPublisher);
    }
}
