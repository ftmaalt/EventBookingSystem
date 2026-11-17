package com.bookngo.bookingservice.concurrency;

import com.bookngo.bookingservice.dto.request.BookingRequest;
import com.bookngo.bookingservice.event.BookingEvent;
import com.bookngo.bookingservice.event.BookingEventType;
import com.bookngo.bookingservice.exception.BusinessConflictsException;
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
import com.bookngo.bookingservice.security.AuthenticatedUser;
import com.bookngo.bookingservice.service.BookingsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Project 3 concurrency guarantees, run against a real PostgreSQL database:
 * pessimistic locking of the session row, FIFO waitlist allocation, and
 * booking events published to Kafka only after the transaction commits.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingConcurrencyIntegrationTest {

    private static final long LATCH_TIMEOUT_SECONDS = 10;
    private static final long RESULT_TIMEOUT_SECONDS = 20;
    private static final AtomicLong USER_IDS = new AtomicLong(System.currentTimeMillis() % 1_000_000_000L * 10);

    @Autowired
    private BookingsService bookingsService;

    @Autowired
    private WaitingListRepository waitingListRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private SessionsRepository sessionsRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingTopic;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @LocalServerPort
    private int port;

    private Long sessionId;
    private AuthenticatedUser customerA;
    private AuthenticatedUser customerB;

    @BeforeEach
    void setUp() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        customerA = newUser("a");
        customerB = newUser("b");

        Sessions session = new Sessions();
        session.setActivityId(1L);
        session.setActivityTitle("Concurrency Test Activity");
        session.setProviderId(1L);
        session.setPricePerPerson(new BigDecimal("10.000"));
        session.setStartTime(LocalDateTime.now().plusDays(7));
        session.setEndTime(LocalDateTime.now().plusDays(7).plusHours(1));
        session.setCapacity(1);
        session.setSpotsLeft(1);
        session.setStatus(SessionStatus.SCHEDULED);
        sessionId = sessionsRepository.saveAndFlush(session).getId();
    }

    private AuthenticatedUser newUser(String name) {
        long id = USER_IDS.incrementAndGet();
        return new AuthenticatedUser(id, name + "-" + id + "@test.com", "USER");
    }

    private void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))));
    }

    private Sessions session() {
        return sessionsRepository.findById(sessionId).orElseThrow();
    }

    private void markSessionFull(int capacity) {
        Sessions session = session();
        session.setCapacity(capacity);
        session.setSpotsLeft(0);
        session.setStatus(SessionStatus.FULL);
        sessionsRepository.saveAndFlush(session);
    }

    private BookingRequest createRequest() {
        BookingRequest request = new BookingRequest();
        request.setSessionId(sessionId);
        request.setParticipants(1);
        request.setBookingType(BookingType.INDIVIDUAL);
        return request;
    }

    private Bookings createConfirmedBooking(AuthenticatedUser user) {
        Bookings booking = new Bookings();
        booking.setUserId(user.id());
        booking.setUserEmail(user.email());
        booking.setSession(session());
        booking.setParticipants(1);
        booking.setBookingType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSubtotal(new BigDecimal("10.000"));
        booking.setDiscountPercent(BigDecimal.ZERO);
        booking.setTotalPrice(new BigDecimal("10.000"));
        booking.setCreatedBy(user.email());
        booking.setUpdatedBy(user.email());
        return bookingsRepository.saveAndFlush(booking);
    }

    private WaitlistEntry createWaitingEntry(AuthenticatedUser user) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUserId(user.id());
        entry.setUserEmail(user.email());
        entry.setSession(session());
        entry.setStatus(WaitListStatus.WAITING);
        return waitingListRepository.saveAndFlush(entry);
    }

    private boolean attemptBooking(AuthenticatedUser user, CountDownLatch ready, CountDownLatch start) throws Exception {
        authenticate(user);
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

    private String attemptCancellation(AuthenticatedUser user, Long bookingId, CountDownLatch ready, CountDownLatch start) throws Exception {
        authenticate(user);
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

    private boolean attemptDuplicateCancellation(AuthenticatedUser user, Long bookingId, CountDownLatch ready, CountDownLatch start) throws Exception {
        try {
            attemptCancellation(user, bookingId, ready, start);
            return true;
        } catch (BusinessConflictsException ex) {
            return false;
        }
    }

    private void cancelAs(AuthenticatedUser user, Long bookingId) {
        authenticate(user);
        try {
            bookingsService.cancelBooking(bookingId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String tokenFor(AuthenticatedUser user) {
        return Jwts.builder()
                .subject(user.email())
                .claim("uid", user.id())
                .claim("role", user.role())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();
    }

    private HttpResponse<String> sendHttpBooking(HttpClient client, String token, CountDownLatch ready, CountDownLatch start) throws Exception {
        String body = """
                {"sessionId": %d, "participants": 1, "bookingType": "INDIVIDUAL"}""".formatted(sessionId);
        ready.countDown();
        if (!start.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Start signal timed out");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private List<BookingEvent> publishedEvents() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, atLeast(0)).send(eq(bookingTopic), anyString(), captor.capture());
        return captor.getAllValues().stream().map(BookingEvent.class::cast).toList();
    }

    private long eventCount(BookingEventType type, Long userId) {
        return publishedEvents().stream().filter(e -> e.eventType() == type && e.userId().equals(userId)).count();
    }

    private static void shutdown(ExecutorService executor, CountDownLatch start) throws InterruptedException {
        start.countDown();
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Two threads book the last seat at the same time: exactly one succeeds,
     * and the database holds one confirmed booking with no seats left.
     */
    @Test
    void onlyOneUserCanBookTheLastSeat() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> attemptBooking(customerA, ready, start));
            Future<Boolean> second = executor.submit(() -> attemptBooking(customerB, ready, start));
            assertTrue(ready.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            start.countDown();

            assertNotEquals(first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            Sessions savedSession = session();
            assertEquals(0, savedSession.getSpotsLeft());
            assertEquals(SessionStatus.FULL, savedSession.getStatus());

            List<Bookings> savedBookings = bookingsRepository.findBySession_Id(sessionId);
            assertEquals(1, savedBookings.size());
            assertEquals(BookingStatus.CONFIRMED, savedBookings.get(0).getStatus());
            assertEquals(1, publishedEvents().size());
        } finally {
            shutdown(executor, start);
        }
    }

    /**
     * Two customers cancel in parallel while two customers wait: each waiting
     * customer receives one seat, in FIFO order, with no seat lost or duplicated.
     */
    @Test
    void simultaneousCancellationsAllocateWaitlist() throws Exception {
        markSessionFull(2);
        Bookings bookingA = createConfirmedBooking(customerA);
        Bookings bookingB = createConfirmedBooking(customerB);

        AuthenticatedUser sara = newUser("sara");
        AuthenticatedUser ali = newUser("ali");
        WaitlistEntry saraEntry = createWaitingEntry(sara);
        WaitlistEntry aliEntry = createWaitingEntry(ali);
        assertTrue(saraEntry.getEntryId() < aliEntry.getEntryId(), "Sara must join the waitlist before Ali");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> attemptCancellation(customerA, bookingA.getBookingId(), ready, start));
            Future<String> second = executor.submit(() -> attemptCancellation(customerB, bookingB.getBookingId(), ready, start));
            assertTrue(ready.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            start.countDown();
            first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertAll("Original bookings",
                    () -> assertEquals(BookingStatus.CANCELLED, bookingsRepository.findById(bookingA.getBookingId()).orElseThrow().getStatus()),
                    () -> assertEquals(BookingStatus.CANCELLED, bookingsRepository.findById(bookingB.getBookingId()).orElseThrow().getStatus()));

            assertAll("Waitlist entries",
                    () -> assertEquals(WaitListStatus.BOOKED, waitingListRepository.findById(saraEntry.getEntryId()).orElseThrow().getStatus()),
                    () -> assertEquals(WaitListStatus.BOOKED, waitingListRepository.findById(aliEntry.getEntryId()).orElseThrow().getStatus()));

            assertAll("Automatic waitlist bookings",
                    () -> assertTrue(bookingsRepository.findByUserIdAndSession_IdAndStatus(sara.id(), sessionId, BookingStatus.CONFIRMED).isPresent()),
                    () -> assertTrue(bookingsRepository.findByUserIdAndSession_IdAndStatus(ali.id(), sessionId, BookingStatus.CONFIRMED).isPresent()));

            List<Bookings> savedBookings = bookingsRepository.findBySession_Id(sessionId);
            assertAll("Booking consistency",
                    () -> assertEquals(4, savedBookings.size()),
                    () -> assertEquals(2L, savedBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count()),
                    () -> assertEquals(2L, savedBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count()));

            Sessions savedSession = session();
            assertAll("Session capacity",
                    () -> assertEquals(2, savedSession.getCapacity()),
                    () -> assertEquals(0, savedSession.getSpotsLeft()),
                    () -> assertEquals(SessionStatus.FULL, savedSession.getStatus()));
        } finally {
            shutdown(executor, start);
        }
    }

    /**
     * A cancellation on a full session gives the seat to the first waiting
     * customer only; the second keeps waiting.
     */
    @Test
    void cancellationAllocatesSeatToFirstWaitlistedUser() {
        markSessionFull(1);
        Bookings bookingA = createConfirmedBooking(customerA);
        AuthenticatedUser sara = newUser("sara");
        AuthenticatedUser ali = newUser("ali");
        WaitlistEntry saraEntry = createWaitingEntry(sara);
        WaitlistEntry aliEntry = createWaitingEntry(ali);
        assertTrue(saraEntry.getEntryId() < aliEntry.getEntryId());

        cancelAs(customerA, bookingA.getBookingId());

        assertEquals(WaitListStatus.BOOKED, waitingListRepository.findById(saraEntry.getEntryId()).orElseThrow().getStatus());
        assertEquals(WaitListStatus.WAITING, waitingListRepository.findById(aliEntry.getEntryId()).orElseThrow().getStatus());
        assertTrue(bookingsRepository.findByUserIdAndSession_IdAndStatus(sara.id(), sessionId, BookingStatus.CONFIRMED).isPresent());
        assertTrue(bookingsRepository.findByUserIdAndSession_IdAndStatus(ali.id(), sessionId, BookingStatus.CONFIRMED).isEmpty());

        Sessions savedSession = session();
        assertEquals(0, savedSession.getSpotsLeft());
        assertEquals(SessionStatus.FULL, savedSession.getStatus());
        assertEquals(BookingStatus.CANCELLED, bookingsRepository.findById(bookingA.getBookingId()).orElseThrow().getStatus());
    }

    /**
     * Two threads cancel the same booking: only one succeeds and exactly one
     * seat is restored.
     */
    @Test
    void concurrentCancellationOfSameBookingRestoresSeatOnlyOnce() throws Exception {
        markSessionFull(1);
        Bookings booking = createConfirmedBooking(customerA);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> attemptDuplicateCancellation(customerA, booking.getBookingId(), ready, start));
            Future<Boolean> second = executor.submit(() -> attemptDuplicateCancellation(customerA, booking.getBookingId(), ready, start));
            assertTrue(ready.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            start.countDown();

            assertNotEquals(first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            Sessions updatedSession = session();
            assertAll("Duplicate cancellation consistency",
                    () -> assertEquals(BookingStatus.CANCELLED, bookingsRepository.findById(booking.getBookingId()).orElseThrow().getStatus()),
                    () -> assertEquals(1, updatedSession.getSpotsLeft()),
                    () -> assertEquals(1, updatedSession.getCapacity()),
                    () -> assertEquals(SessionStatus.SCHEDULED, updatedSession.getStatus()),
                    () -> assertEquals(1, bookingsRepository.findBySession_Id(sessionId).size()),
                    () -> assertEquals(1L, eventCount(BookingEventType.BOOKING_CANCELLED, customerA.id())));
        } finally {
            shutdown(executor, start);
        }
    }

    /**
     * Two authenticated users POST /api/bookings for the last seat at the same
     * time: the API answers one 201 and one 409, through the JWT filter,
     * controller, service and PostgreSQL layers.
     */
    @Test
    void concurrentHttpBookingsAllowOnlyOneReservation() throws Exception {
        String tokenA = tokenFor(customerA);
        String tokenB = tokenFor(customerB);
        HttpClient client = HttpClient.newHttpClient();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<HttpResponse<String>> first = executor.submit(() -> sendHttpBooking(client, tokenA, ready, start));
            Future<HttpResponse<String>> second = executor.submit(() -> sendHttpBooking(client, tokenB, ready, start));
            assertTrue(ready.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            start.countDown();

            HttpResponse<String> responseA = first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            HttpResponse<String> responseB = second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<Integer> statuses = List.of(responseA.statusCode(), responseB.statusCode()).stream().sorted().toList();
            assertEquals(List.of(201, 409), statuses, "Responses: A=" + responseA.body() + " B=" + responseB.body());

            List<Bookings> bookings = bookingsRepository.findBySession_Id(sessionId);
            assertEquals(1, bookings.size());
            assertEquals(BookingStatus.CONFIRMED, bookings.get(0).getStatus());

            Sessions savedSession = session();
            assertEquals(1, savedSession.getCapacity());
            assertEquals(0, savedSession.getSpotsLeft());
            assertEquals(SessionStatus.FULL, savedSession.getStatus());
        } finally {
            shutdown(executor, start);
        }
    }

    /**
     * A booking event raised inside a transaction that rolls back is never
     * published, and the booking is not persisted.
     */
    @Test
    void rolledBackBookingDoesNotPublishEvent() {
        Long bookingId = new TransactionTemplate(transactionManager).execute(status -> {
            Bookings booking = createConfirmedBooking(customerA);
            eventPublisher.publishEvent(BookingEvent.of(booking, BookingEventType.BOOKING_CONFIRMED));
            status.setRollbackOnly();
            return booking.getBookingId();
        });

        assertNotNull(bookingId);
        assertTrue(bookingsRepository.findById(bookingId).isEmpty(), "Booking should not exist after rollback");
        verify(kafkaTemplate, after(1000).never()).send(anyString(), anyString(), any());
    }

    /**
     * A booking event is published to Kafka exactly once, and only after the
     * transaction commits.
     */
    @Test
    void committedBookingPublishesEventAfterCommit() {
        Long bookingId = new TransactionTemplate(transactionManager).execute(status -> {
            Bookings booking = createConfirmedBooking(customerA);
            eventPublisher.publishEvent(BookingEvent.of(booking, BookingEventType.BOOKING_CONFIRMED));
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
            return booking.getBookingId();
        });

        assertNotNull(bookingId);
        assertTrue(bookingsRepository.findById(bookingId).isPresent());
        verify(kafkaTemplate, timeout(5000).times(1)).send(eq(bookingTopic), eq(String.valueOf(bookingId)),
                argThat(e -> e instanceof BookingEvent event && event.eventType() == BookingEventType.BOOKING_CONFIRMED));
    }

    /**
     * Cancelling a booking restores its seat and publishes BOOKING_CANCELLED
     * after the cancellation commits.
     */
    @Test
    void cancelledBookingPublishesEventAfterCommit() {
        markSessionFull(1);
        Bookings booking = createConfirmedBooking(customerA);

        cancelAs(customerA, booking.getBookingId());

        Sessions updatedSession = session();
        assertAll("Cancellation consistency",
                () -> assertEquals(BookingStatus.CANCELLED, bookingsRepository.findById(booking.getBookingId()).orElseThrow().getStatus()),
                () -> assertEquals(1, updatedSession.getSpotsLeft()),
                () -> assertEquals(SessionStatus.SCHEDULED, updatedSession.getStatus()));

        verify(kafkaTemplate, timeout(5000).times(1)).send(eq(bookingTopic), eq(String.valueOf(booking.getBookingId())),
                argThat(e -> e instanceof BookingEvent event && event.eventType() == BookingEventType.BOOKING_CANCELLED));
    }

    /**
     * When a released seat goes to the first waiting customer, that customer gets
     * WAITLIST_ALLOCATED, the cancelling customer gets BOOKING_CANCELLED, and the
     * customer still waiting gets nothing.
     */
    @Test
    void waitlistAllocationPublishesEventsAfterCommit() {
        markSessionFull(1);
        Bookings originalBooking = createConfirmedBooking(customerA);
        AuthenticatedUser sara = newUser("sara");
        AuthenticatedUser ali = newUser("ali");
        WaitlistEntry saraEntry = createWaitingEntry(sara);
        WaitlistEntry aliEntry = createWaitingEntry(ali);
        assertTrue(saraEntry.getEntryId() < aliEntry.getEntryId());

        cancelAs(customerA, originalBooking.getBookingId());

        assertEquals(WaitListStatus.BOOKED, waitingListRepository.findById(saraEntry.getEntryId()).orElseThrow().getStatus());
        assertEquals(WaitListStatus.WAITING, waitingListRepository.findById(aliEntry.getEntryId()).orElseThrow().getStatus());
        Bookings saraBooking = bookingsRepository.findByUserIdAndSession_IdAndStatus(sara.id(), sessionId, BookingStatus.CONFIRMED).orElseThrow();

        Sessions updatedSession = session();
        assertEquals(0, updatedSession.getSpotsLeft());
        assertEquals(SessionStatus.FULL, updatedSession.getStatus());

        verify(kafkaTemplate, timeout(5000).times(2)).send(eq(bookingTopic), anyString(), any());
        assertEquals(1L, eventCount(BookingEventType.WAITLIST_ALLOCATED, sara.id()));
        assertEquals(1L, eventCount(BookingEventType.BOOKING_CANCELLED, customerA.id()));
        assertEquals(0L, publishedEvents().stream().filter(e -> e.userId().equals(ali.id())).count());
        assertTrue(publishedEvents().stream().anyMatch(e -> e.bookingId().equals(saraBooking.getBookingId())));
    }

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (sessionId == null) {
                return;
            }
            waitingListRepository.deleteAll(waitingListRepository.findAll().stream()
                    .filter(entry -> entry.getSession() != null && sessionId.equals(entry.getSession().getId()))
                    .toList());
            bookingsRepository.deleteAll(bookingsRepository.findBySession_Id(sessionId));
            sessionsRepository.findById(sessionId).ifPresent(sessionsRepository::delete);
        });
        sessionId = null;
    }
}
