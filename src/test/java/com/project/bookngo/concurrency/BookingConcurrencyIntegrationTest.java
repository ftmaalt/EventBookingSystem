
package com.project.bookngo.concurrency;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.model.request.BookingRequest;
import com.project.bookngo.repository.*;
import com.project.bookngo.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingsService bookingsService;

    @Autowired
    private WaitingListRepository waitingListRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private SessionsRepository sessionsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ActivitiesRepository activitiesRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private ReminderService reminderService;

    private Long sessionId;
    private String firstEmail;
    private String secondEmail;

    private static final long LATCH_TIMEOUT_SECONDS=10;
    private static final long RESULT_TIMEOUT_SECONDS=20;


    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        User provider = createUser("Provider", "provider-" + unique + "@test.com", UserRole.PROVIDER);

        User customerA = createUser("Customer A", "a-" + unique + "@test.com", UserRole.USER);

        User customerB = createUser("Customer B", "b-" + unique + "@test.com", UserRole.USER);

        firstEmail = customerA.getEmail();
        secondEmail = customerB.getEmail();

        Category category = new Category();
        category.setCategoryName("Concurrency-" + unique);
        category.setDescription("Concurrency test category");
        category.setActive(true);
        category = categoryRepository.save(category);

        Location location = new Location();
        location.setName("Test Location");
        location.setAddress("Test Road");
        location.setCity("Manama");
        location.setLatitude(26.2235);
        location.setLongitude(50.5876);
        location.setActive(true);
        location.setProvider(provider);
        location = locationRepository.save(location);

        Activities activity = new Activities();
        activity.setTitle("Concurrency Test Activity");
        activity.setDescription("Test activity for booking concurrency");
        activity.setPricePerPerson(new BigDecimal("10.000"));
        activity.setDurationMinutes(60);
        activity.setStatus(ActivityStatus.ACTIVE);
        activity.setProvider(provider);
        activity.setCategory(category);
        activity.setLocation(location);
        activity = activitiesRepository.save(activity);

        Sessions session = new Sessions();
        session.setActivity(activity);
        session.setStartTime(LocalDateTime.now().plusDays(7));
        session.setEndTime(LocalDateTime.now().plusDays(7).plusHours(1));
        session.setCapacity(1);
        session.setSpotsLeft(1);
        session.setStatus(SessionStatus.SCHEDULED);

        sessionId = sessionsRepository.saveAndFlush(session).getId();
    }

    private User createUser(String name, String email, UserRole role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash("integration-test-only");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(false);
        user.setStrikeCount((short) 0);
        user.setConsecutiveViolations((short) 0);

        return usersRepository.save(user);
    }

    private BookingRequest createRequest() {
        BookingRequest request = new BookingRequest();
        request.setSessionId(sessionId);
        request.setParticipants(1);
        request.setBookingType(BookingType.INDIVIDUAL);
        return request;
    }

    private boolean attemptBooking(String email, CountDownLatch ready, CountDownLatch start) throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            ready.countDown();

            if (!start.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start signal timed out");
            }
            bookingsService.createBooking(createRequest());
            return true;

        } catch (BusinessConflictsException | ObjectOptimisticLockingFailureException ex) {
            return false;

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void onlyOneUserCanBookTheLastSeat() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(
                    () -> attemptBooking(firstEmail, ready, start));

            Future<Boolean> second = executor.submit(
                    () -> attemptBooking(secondEmail, ready, start));

            assertTrue(ready.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            start.countDown();

            boolean firstSucceeded = first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertNotEquals(firstSucceeded, secondSucceeded);

            Sessions savedSession = sessionsRepository.findById(sessionId).orElseThrow();

            assertEquals(0, savedSession.getSpotsLeft());
            assertEquals(SessionStatus.FULL, savedSession.getStatus());

            List<Bookings> savedBookings = bookingsRepository.findBySessionId(sessionId);

            assertEquals(1, savedBookings.size());
            assertEquals(1, savedBookings.get(0).getParticipants());
            assertEquals(
                    BookingStatus.CONFIRMED,
                    savedBookings.get(0).getStatus());

        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    private Bookings createConfirmedBooking(User user, Sessions session) {
        Bookings booking = new Bookings();

        booking.setUser(user);
        booking.setSession(session);
        booking.setParticipants(1);
        booking.setBookingType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        booking.setSubtotal(new BigDecimal("10.000"));
        booking.setDiscountPercent(BigDecimal.ZERO);
        booking.setTotalPrice(new BigDecimal("10.000"));

        booking.setCreatedBy(user.getEmail());
        booking.setUpdatedBy(user.getEmail());

        return bookingsRepository.saveAndFlush(booking);
    }

    private WaitlistEntry createWaitingEntry(User user, Sessions session) {
        WaitlistEntry entry = new WaitlistEntry();

        entry.setUser(user);
        entry.setSession(session);
        entry.setStatus(WaitListStatus.WAITING);

        return waitingListRepository.saveAndFlush(entry);
    }

    private String attemptCancellation(String email, Long bookingId, CountDownLatch ready, CountDownLatch start) throws Exception {

        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            ready.countDown();

            if (!start.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start signal timed out");
            }

            return bookingsService.cancelBooking(bookingId);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void simultaneousCancellationsAllocateWaitlist() throws Exception {
        Sessions session = sessionsRepository.findById(sessionId)
                .orElseThrow();

        session.setCapacity(2);
        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);
        sessionsRepository.saveAndFlush(session);

        User customerA = usersRepository.findUserByEmail(firstEmail);
        User customerB = usersRepository.findUserByEmail(secondEmail);

        Bookings bookingA = createConfirmedBooking(customerA, session);
        Bookings bookingB = createConfirmedBooking(customerB, session);

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User sara = createUser(
                "Sara", "sara-" + suffix + "@test.com", UserRole.USER
        );

        User ali = createUser(
                "Ali", "ali-" + suffix + "@test.com", UserRole.USER
        );

        WaitlistEntry saraEntry = createWaitingEntry(sara, session);
        WaitlistEntry aliEntry = createWaitingEntry(ali, session);

        assertTrue(saraEntry.getEntryId() < aliEntry.getEntryId(), "Sara must join the waitlist before ali");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<String> first = executor.submit(
                    () -> attemptCancellation(firstEmail,bookingA.getBookingId(), ready, start));

            Future<String> second = executor.submit(
                    () -> attemptCancellation(secondEmail,bookingB.getBookingId(), ready, start));
            assertTrue(ready.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            start.countDown();

            first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);


            Bookings cancelledA= bookingsRepository.findById(bookingA.getBookingId()).orElseThrow();
            Bookings cancelledB= bookingsRepository.findById(bookingB.getBookingId()).orElseThrow();

            assertAll("Original Bookings", () -> assertEquals(BookingStatus.CANCELLED, cancelledA.getStatus()),
                    () ->  assertEquals(BookingStatus.CANCELLED, cancelledB.getStatus()));

            WaitlistEntry updatedSara= waitingListRepository.findById(saraEntry.getEntryId()).orElseThrow();
            WaitlistEntry updatedAli= waitingListRepository.findById(aliEntry.getEntryId()).orElseThrow();

            assertAll("Waitlist Entries",
                    () -> assertEquals(WaitListStatus.BOOKED, updatedSara.getStatus()),
                    () -> assertEquals(WaitListStatus.BOOKED, updatedAli.getStatus()));

            assertAll("Automatic waitlist bookings",
                    () -> assertTrue(bookingsRepository.findByUserIdAndSessionIdAndStatus(sara.getId(),
                            sessionId,
                            BookingStatus.CONFIRMED).isPresent(),
                            "Sara should have a confirmed booking"),

                    () -> assertTrue(bookingsRepository.findByUserIdAndSessionIdAndStatus(ali.getId(),
                            sessionId, BookingStatus.CONFIRMED).isPresent(),
                            "Ali should have a confirmed booking"
                    ));
            List<Bookings> savedBookings = bookingsRepository.findBySessionId(sessionId);

            long confirmedCount = savedBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();

            long cancelledCount = savedBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

            assertAll("Booking consistency",
                    () -> assertEquals(4, savedBookings.size()),
                    () -> assertEquals(2L, confirmedCount),
                    () -> assertEquals(2L, cancelledCount));

            Sessions savedSession = sessionsRepository.findById(sessionId)
                    .orElseThrow();

            assertAll("Session capacity",
                    () -> assertEquals(2, savedSession.getCapacity()),
                    () -> assertEquals(0, savedSession.getSpotsLeft()),
                    () -> assertEquals(SessionStatus.FULL, savedSession.getStatus()));
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void cancellationAllocatesSeatToFirstWaitlistedUser() {
        Sessions session = sessionsRepository.findById(sessionId).orElseThrow();
        User customerA = usersRepository.findUserByEmail(firstEmail);

        Bookings bookingA = createConfirmedBooking(customerA, session);
        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);
        sessionsRepository.saveAndFlush(session);

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User sara = createUser(
                "Sara", "sara-" + suffix + "@test.com", UserRole.USER
        );

        User ali = createUser(
                "Ali", "ali-" + suffix + "@test.com", UserRole.USER
        );

        WaitlistEntry saraEntry = createWaitingEntry(sara, session);

        WaitlistEntry aliEntry = createWaitingEntry(ali, session);

        assertTrue(saraEntry.getEntryId() < aliEntry.getEntryId());

        var auth = new UsernamePasswordAuthenticationToken(
                firstEmail,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            bookingsService.cancelBooking(bookingA.getBookingId());
        } finally {
            SecurityContextHolder.clearContext();
        }

        WaitlistEntry updatedSara = waitingListRepository.findById(saraEntry.getEntryId()).orElseThrow();
        WaitlistEntry updatedAli = waitingListRepository.findById(aliEntry.getEntryId()).orElseThrow();

        assertEquals(WaitListStatus.BOOKED, updatedSara.getStatus());

        assertEquals(WaitListStatus.WAITING, updatedAli.getStatus());

        assertTrue(bookingsRepository.findByUserIdAndSessionIdAndStatus(sara.getId(), sessionId, BookingStatus.CONFIRMED).isPresent());
        assertTrue(bookingsRepository.findByUserIdAndSessionIdAndStatus(ali.getId(), sessionId, BookingStatus.CONFIRMED).isEmpty());

        Sessions savedSession = sessionsRepository.findById(sessionId).orElseThrow();

        assertEquals(0, savedSession.getSpotsLeft());
        assertEquals(SessionStatus.FULL,savedSession.getStatus());

        Bookings cancelledBooking = bookingsRepository.findById(bookingA.getBookingId()).orElseThrow();

        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());


    }



}
