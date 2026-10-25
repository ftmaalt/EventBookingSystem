
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

    // Keep real database logic, but prevent external side effects.
    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private ReminderService reminderService;

    private Long sessionId;
    private String firstEmail;
    private String secondEmail;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        User provider = createUser(
                "Provider", "provider-" + unique + "@test.com",
                UserRole.PROVIDER
        );

        User customerA = createUser(
                "Customer A", "a-" + unique + "@test.com",
                UserRole.USER
        );

        User customerB = createUser(
                "Customer B", "b-" + unique + "@test.com",
                UserRole.USER
        );

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

    private boolean attemptBooking(
            String email,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {

        // SecurityContext is thread-local.
        // Each worker authenticates as a different customer.
        var auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start signal timed out");
            }

            bookingsService.createBooking(createRequest());
            return true;

        } catch (BusinessConflictsException ex) {
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
                    () -> attemptBooking(firstEmail, ready, start)
            );

            Future<Boolean> second = executor.submit(
                    () -> attemptBooking(secondEmail, ready, start)
            );

            // Ensure both workers are ready before releasing them.
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            boolean firstSucceeded = first.get(20, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(20, TimeUnit.SECONDS);

            // Exactly one customer should successfully book.
            assertNotEquals(firstSucceeded, secondSucceeded);

            // Query the actual PostgreSQL database.
            Sessions savedSession = sessionsRepository
                    .findById(sessionId)
                    .orElseThrow();

            assertEquals(0, savedSession.getSpotsLeft());
            assertEquals(SessionStatus.FULL, savedSession.getStatus());

            List<Bookings> savedBookings =
                    bookingsRepository.findBySessionId(sessionId);

            assertEquals(1, savedBookings.size());
            assertEquals(1, savedBookings.get(0).getParticipants());
            assertEquals(
                    BookingStatus.CONFIRMED,
                    savedBookings.get(0).getStatus()
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

}
