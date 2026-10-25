
package com.project.bookngo.service;

import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private WaitingService waitingService;

    private Sessions session;
    private User firstUser;
    private User secondUser;
    private WaitlistEntry firstEntry;
    private WaitlistEntry secondEntry;

    @BeforeEach
    void setUp() {

        Activities activity = new Activities();
        activity.setStatus(ActivityStatus.ACTIVE);
        activity.setPricePerPerson(new BigDecimal("15.000"));

        session = new Sessions();
        session.setId(100L);
        session.setActivity(activity);
        session.setCapacity(5);
        session.setSpotsLeft(1);
        session.setStatus(SessionStatus.SCHEDULED);
        session.setStartTime(LocalDateTime.now().plusDays(3));

        firstUser = new User();
        firstUser.setId(1L);
        firstUser.setEmail("first@test.com");
        firstUser.setRole(UserRole.USER);
        firstUser.setStatus(UserStatus.ACTIVE);

        secondUser = new User();
        secondUser.setId(2L);
        secondUser.setEmail("second@test.com");
        secondUser.setRole(UserRole.USER);
        secondUser.setStatus(UserStatus.ACTIVE);

        firstEntry = new WaitlistEntry();
        firstEntry.setEntryId(10L);
        firstEntry.setUser(firstUser);
        firstEntry.setSession(session);
        firstEntry.setStatus(WaitListStatus.WAITING);

        secondEntry = new WaitlistEntry();
        secondEntry.setEntryId(11L);
        secondEntry.setUser(secondUser);
        secondEntry.setSession(session);
        secondEntry.setStatus(WaitListStatus.WAITING);
    }

    @Test
    void shouldAllocateFirstWaitingCustomer() {

        when(waitlistRepository
                .findBySessionIdAndStatusOrderByEntryIdAsc(
                        100L, WaitListStatus.WAITING
                ))
                .thenReturn(List.of(firstEntry, secondEntry));

        waitingService.allocateNextWaitlistedUser(session);

        // First person gets the only available seat.
        assertEquals(WaitListStatus.BOOKED, firstEntry.getStatus());
        assertEquals(WaitListStatus.WAITING, secondEntry.getStatus());

        assertEquals(0, session.getSpotsLeft());
        assertEquals(SessionStatus.FULL, session.getStatus());

        ArgumentCaptor<Bookings> captor =
                ArgumentCaptor.forClass(Bookings.class);

        verify(bookingsRepository, times(1))
                .save(captor.capture());

        Bookings savedBooking = captor.getValue();

        assertEquals(firstUser.getId(), savedBooking.getUser().getId());
        assertEquals(1, savedBooking.getParticipants());
        assertEquals(BookingType.INDIVIDUAL,
                savedBooking.getBookingType());
        assertEquals(BookingStatus.CONFIRMED,
                savedBooking.getStatus());
        assertEquals(PaymentStatus.PENDING_PAYMENT,
                savedBooking.getPaymentStatus());

        assertEquals(
                0,
                new BigDecimal("15.000")
                        .compareTo(savedBooking.getTotalPrice())
        );

        verify(waitlistRepository).save(firstEntry);
        verify(waitlistRepository, never()).save(secondEntry);
    }

    @Test
    void shouldNotAllocateWithoutAvailableSeats() {

        session.setSpotsLeft(0);

        waitingService.allocateNextWaitlistedUser(session);

        verifyNoInteractions(bookingsRepository);
        verifyNoInteractions(waitlistRepository);
    }

    @Test
    void shouldSkipInactiveCustomer() {

        firstUser.setStatus(UserStatus.DEACTIVATED);

        when(waitlistRepository
                .findBySessionIdAndStatusOrderByEntryIdAsc(
                        100L, WaitListStatus.WAITING
                ))
                .thenReturn(List.of(firstEntry, secondEntry));

        waitingService.allocateNextWaitlistedUser(session);

        assertEquals(WaitListStatus.WAITING, firstEntry.getStatus());
        assertEquals(WaitListStatus.BOOKED, secondEntry.getStatus());

        ArgumentCaptor<Bookings> captor =
                ArgumentCaptor.forClass(Bookings.class);

        verify(bookingsRepository).save(captor.capture());

        assertEquals(secondUser.getId(),
                captor.getValue().getUser().getId());

        assertEquals(0, session.getSpotsLeft());
    }

    @Test
    void shouldNotAllocateAfterSessionStarts() {

        session.setStartTime(LocalDateTime.now().minusMinutes(1));

        waitingService.allocateNextWaitlistedUser(session);

        verifyNoInteractions(bookingsRepository);
        verifyNoInteractions(waitlistRepository);
    }
}
