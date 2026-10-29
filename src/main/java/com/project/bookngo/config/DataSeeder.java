package com.project.bookngo.config;

import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SCENARIO_ACTION = "PROJECT3_DEMO_SCENARIO_V1";

    private final UsersRepository          usersRepository;
    private final CategoryRepository       categoryRepository;
    private final LocationRepository       locationRepository;
    private final ActivitiesRepository     activitiesRepository;
    private final SessionsRepository       sessionsRepository;
    private final BookingsRepository       bookingsRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final WaitingListRepository    waitingListRepository;
    private final AuditLogRepository       auditLogRepository;
    private final PasswordEncoder          passwordEncoder;

    @Value("${bookngo.demo.password:BookNGo123!}")
    private String demoPassword;

    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void run(String... args) {

        createUser("Admin User",    "admin@bookngo.com",     "30000001", UserRole.ADMIN,    UserStatus.ACTIVE);
        User provider1 = createUser("Layla Ahmed",  "provider@bookngo.com",  "30000002", UserRole.PROVIDER, UserStatus.ACTIVE);
        User provider2 = createUser("Omar Hassan",  "provider2@bookngo.com", "30000003", UserRole.PROVIDER, UserStatus.ACTIVE);
        createUser("Sara Mohamed",  "customer@bookngo.com",  "30000004", UserRole.USER,     UserStatus.ACTIVE);
        createUser("Ali Ahmed",     "customer2@bookngo.com", "30000005", UserRole.USER,     UserStatus.ACTIVE);
        createUser("Noora Khalid",  "customer3@bookngo.com", "30000006", UserRole.USER,     UserStatus.DEACTIVATED);
        User provider3 = createUser("Hessa Ali",    "provider3@bookngo.com", "30000007", UserRole.PROVIDER, UserStatus.ACTIVE);

        createProviderProfile(provider1, "Layla Experiences", "Creative workshops and guided experiences.", "30000002");
        createProviderProfile(provider2, "Omar Adventures",   "Outdoor and coastal experiences.",           "30000003");
        createProviderProfile(provider3, "Hessa Wellness",    "Yoga and wellness classes.",                 "30000007");

        String[] accounts = {
                "jmeter1", "jmeter2",
                "waitlist1", "waitlist2", "waitlist3",
                "booking1", "booking2",
                "demo1", "demo2", "demo3", "demo4", "demo5",
                "inactive", "async"
        };
        for (int i = 0; i < accounts.length; i++) {
            createUser(
                    "Demo " + accounts[i],
                    accounts[i] + "@bookngo.com",
                    String.valueOf(31000001 + i),
                    UserRole.USER,
                    accounts[i].equals("inactive") ? UserStatus.DEACTIVATED : UserStatus.ACTIVE
            );
        }

        // resolve users needed for scenario setup
        User booking1  = usersRepository.findUserByEmail("booking1@bookngo.com");
        User booking2  = usersRepository.findUserByEmail("booking2@bookngo.com");
        User jmeter1   = usersRepository.findUserByEmail("jmeter1@bookngo.com");
        User jmeter2   = usersRepository.findUserByEmail("jmeter2@bookngo.com");
        List<User> waiting = List.of(
                usersRepository.findUserByEmail("waitlist1@bookngo.com"),
                usersRepository.findUserByEmail("waitlist2@bookngo.com"),
                usersRepository.findUserByEmail("waitlist3@bookngo.com")
        );

        // ── categories ────────────────────────────────────────────────────────
        Category outdoor  = createCategory("Outdoor Adventures",  "Kayaking, camping, and outdoor experiences.");
        Category creative = createCategory("Creative Workshops",   "Pottery, painting, and creative classes.");
        Category fitness  = createCategory("Fitness & Wellness",   "Yoga, fitness, and wellness activities.");
        Category food     = createCategory("Food & Cooking",       "Cooking classes and culinary experiences.");

        // ── locations ─────────────────────────────────────────────────────────
        Location manama = createLocation("BookNGo Creative Studio",   "Exhibition Road",  "Manama",               26.242552, 50.553782, provider1);
        Location amwaj  = createLocation("Amwaj Adventure Center",    "Amwaj Islands",    "Muharraq",             26.2870,   50.6640,   provider2);
        Location riffa  = createLocation("Riffa Activity Center",     "Riffa",            "Southern Governorate", 26.1300,   50.5550,   provider1);
        Location seef   = createLocation("Hessa Wellness Studio",     "Seef District",    "Manama",               26.237,    50.536,    provider3);

        // ── activities ────────────────────────────────────────────────────────
        Activities kayaking = createActivity("Sunset Kayaking Tour",            "Guided kayaking along the coast.",          "15.000", 90,  outdoor,  amwaj,  provider2);
        Activities pottery  = createActivity("Beginner Pottery Workshop",       "Learn pottery with an instructor.",         "20.000", 120, creative, manama, provider1);
        Activities yoga     = createActivity("Morning Yoga Session",            "Beginner-friendly yoga class.",             "8.000",  60,  fitness,  riffa,  provider1);
        Activities cooking  = createActivity("Traditional Bahraini Cooking",    "Prepare traditional Bahraini dishes.",      "25.000", 150, food,     manama, provider1);
        Activities art      = createActivity("Project 3 Watercolour Evening",   "Paint a Bahraini sunset in a relaxed studio.", "18.000", 90, creative, seef, provider3);

        // ══════════════════════════════════════════════════════════════════════
        //  SCENARIOS
        //  Each call is idempotent — the audit-log marker prevents recreation.
        // ══════════════════════════════════════════════════════════════════════
        logger.info("========== BOOKNGO PROJECT 3 DEMO DATA ==========");

        // ── original scenarios (preserved) ────────────────────────────────────
        scenario("JMETER_RACE_01",          kayaking, 5,  17, 1, List.of(),              List.of(),               1);
        scenario("JMETER_RACE_02",          kayaking, 6,  17, 1, List.of(),              List.of(),               1);
        scenario("JMETER_RACE_03",          kayaking, 7,  17, 1, List.of(),              List.of(),               1);
        scenario("FIFO_WAITLIST",           pottery,  8,  15, 1, List.of(booking1),      waiting,                 1);
        scenario("CONCURRENT_CANCELLATION", pottery,  9,  15, 2, List.of(booking1, booking2), waiting.subList(0, 2), 1);
        scenario("DUPLICATE_CANCELLATION",  kayaking, 10, 17, 1, List.of(booking1),      List.of(),               1);
        scenario("CAPACITY_VALIDATION",     cooking,  11, 14, 5, List.of(booking2),      List.of(),               4);
        scenario("BROWSE_AVAILABLE",        yoga,     12, 9,  12, List.of(),             List.of(),               1);
        scenario("BROWSE_PARTIAL",          cooking,  13, 14, 8,  List.of(booking1),     List.of(),               2);
        scenario("BROWSE_FULL",             art,      14, 18, 4,  List.of(booking2),     List.of(),               4);
        scenario("BROWSE_KAYAKING",         kayaking, 20, 17, 8,  List.of(),             List.of(),               1);
        scenario("BROWSE_POTTERY",          pottery,  25, 15, 10, List.of(),             List.of(),               1);
        scenario("ASYNC_NOTIFICATION",      art,      15, 18, 6,  List.of(),             List.of(),               1);

        // ── NEW: presentation-day demo sessions ───────────────────────────────


        scenario("DEMO_JMETER_FRESH", kayaking, 1, 10, 1, List.of(), List.of(), 1);

        scenario("DEMO_WAITLIST_LIVE", pottery, 2, 10, 1, List.of(booking1), waiting, 1);


        scenario("DEMO_ASYNC_NOTIFY", art, 3, 14, 2, List.of(), List.of(), 1);

        // ──────────────────────────────────────────────────────────────────────
        logger.info("");
        logger.info("PRESENTATION DAY QUICK REFERENCE");
        logger.info("  JMeter race target  → DEMO_JMETER_FRESH  (capacity 1, 0 bookings)");
        logger.info("  Waitlist live demo  → DEMO_WAITLIST_LIVE  (capacity 1, booking1 holds seat, 3 waiters in FIFO)");
        logger.info("  Async notify demo   → DEMO_ASYNC_NOTIFY   (capacity 2, book as async@bookngo.com live)");
        logger.info("  JMeter credentials  → jmeter1@bookngo.com / jmeter2@bookngo.com  pw: {}", demoPassword);
        logger.info("  Waitlist holder     → booking1@bookngo.com  pw: {}", demoPassword);
        logger.info("  Async customer      → async@bookngo.com     pw: {}", demoPassword);
        logger.info("===================================================");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Core scenario builder — idempotent via audit_log marker
    // ══════════════════════════════════════════════════════════════════════════

    private void scenario(
            String key,
            Activities activity,
            int day, int hour,
            int capacity,
            List<User> holders,
            List<User> waiting,
            int participants) {

        AuditLog marker = auditLogRepository
                .findFirstByActionAndDetails(SCENARIO_ACTION, key)
                .orElse(null);

        Sessions session;

        if (marker != null) {
            // Scenario already seeded — just log current state.
            session = sessionsRepository.findById(marker.getTargetId()).orElse(null);
            if (session == null) {
                logger.warn("{}: original session {} is missing. Delete the audit_log marker and restart to recreate it.", key, marker.getTargetId());
                return;
            }
        } else {
            // First run — create the session.
            session = new Sessions();
            session.setActivity(activity);
            session.setStartTime(LocalDateTime.of(2026, 11, day, hour, 0));
            session.setEndTime(session.getStartTime().plusMinutes(activity.getDurationMinutes()));
            session.setCapacity(capacity);
            session.setSpotsLeft(capacity);
            session.setStatus(SessionStatus.SCHEDULED);
            session = sessionsRepository.save(session);

            for (User holder : holders) {
                createBooking(holder, session, participants);
            }

            for (User user : waiting) {
                if (session.getSpotsLeft() != 0) {
                    throw new IllegalStateException(
                            "Scenario " + key + ": waitlist entries require a FULL session (spotsLeft must be 0 before adding waiters)."
                    );
                }
                WaitlistEntry entry = new WaitlistEntry();
                entry.setUser(user);
                entry.setSession(session);
                entry.setStatus(WaitListStatus.WAITING);
                waitingListRepository.saveAndFlush(entry); // FIFO relies on auto-increment entryId ASC
            }

            auditLogRepository.save(new AuditLog(
                    SCENARIO_ACTION, "DEV_SEEDER", "Sessions",
                    session.getId(), key, LocalDateTime.now()
            ));
        }

        // ── always log current state so you can confirm readiness at startup ──
        logger.info("{}: Session ID={}, activity={}, start={}, capacity={}, spotsLeft={}, status={}",
                key,
                session.getId(),
                session.getActivity().getTitle(),
                session.getStartTime(),
                session.getCapacity(),
                session.getSpotsLeft(),
                session.getStatus()
        );
        for (Bookings b : bookingsRepository.findBySessionId(session.getId())) {
            logger.info("  ↳ Booking ID={} | user={} | participants={} | status={}",
                    b.getBookingId(), b.getUser().getEmail(), b.getParticipants(), b.getStatus());
        }
        for (WaitlistEntry e : waitingListRepository
                .findBySessionIdAndStatusOrderByEntryIdAsc(session.getId(), WaitListStatus.WAITING)) {
            logger.info("  ↳ Waitlist entry ID={} | user={} | status={}",
                    e.getEntryId(), e.getUser().getEmail(), e.getStatus());
        }

        // Warn if a JMeter or fresh-race scenario is no longer in a usable state.
        if ((key.startsWith("JMETER") || key.equals("DEMO_JMETER_FRESH"))
                && (session.getSpotsLeft() != 1
                || session.getStatus() != SessionStatus.SCHEDULED
                || !bookingsRepository.findBySessionId(session.getId()).isEmpty()
                || waitingListRepository.existsBySessionIdAndStatus(session.getId(), WaitListStatus.WAITING))) {
            logger.warn("⚠  {} has been consumed. Delete its audit_log marker and session row, then restart to get a fresh race target.", key);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Entity helpers — all idempotent
    // ══════════════════════════════════════════════════════════════════════════

    private User createUser(String name, String email, String phone, UserRole role, UserStatus status) {
        User existing = usersRepository.findUserByEmail(email);
        if (existing != null) return existing;

        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setRole(role);
        user.setStatus(status);
        user.setMustChangePassword(false);
        user.setStrikeCount((short) 0);
        user.setConsecutiveViolations((short) 0);
        return usersRepository.save(user);
    }

    private ProviderProfile createProviderProfile(User provider, String businessName, String description, String phone) {
        ProviderProfile existing = providerProfileRepository.findByUserId(provider.getId()).orElse(null);
        if (existing != null) return existing;

        ProviderProfile profile = new ProviderProfile();
        profile.setUser(provider);
        profile.setBusinessName(businessName);
        profile.setDescription(description);
        profile.setPhone(phone);
        return providerProfileRepository.save(profile);
    }

    private Category createCategory(String name, String description) {
        Category existing = categoryRepository.findByCategoryName(name);
        if (existing != null) return existing;

        Category category = new Category();
        category.setCategoryName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    private Location createLocation(String name, String address, String city,
                                    double latitude, double longitude, User provider) {
        Location existing = locationRepository.findByProviderIdAndName(provider.getId(), name).orElse(null);
        if (existing != null) return existing; // Preserve manually edited map coordinates.

        Location location = new Location();
        location.setName(name);
        location.setAddress(address);
        location.setCity(city);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setProvider(provider);
        location.setActive(true);
        return locationRepository.save(location);
    }

    private Activities createActivity(String title, String description, String price,
                                      int durationMinutes, Category category,
                                      Location location, User provider) {
        Activities existing = activitiesRepository.findByProviderIdAndTitle(provider.getId(), title).orElse(null);
        if (existing != null) return existing;

        Activities activity = new Activities();
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setPricePerPerson(new BigDecimal(price));
        activity.setDurationMinutes(durationMinutes);
        activity.setCategory(category);
        activity.setLocation(location);
        activity.setProvider(provider);
        activity.setStatus(ActivityStatus.ACTIVE);
        activity.setCreatedBy(provider.getEmail());
        activity.setUpdatedBy(provider.getEmail());
        return activitiesRepository.save(activity);
    }

    private Bookings createBooking(User customer, Sessions session, int participants) {
        if (bookingsRepository
                .findByUserIdAndSessionIdAndStatus(customer.getId(), session.getId(), BookingStatus.CONFIRMED)
                .isPresent()) {
            throw new IllegalStateException("Duplicate demo booking for " + customer.getEmail());
        }
        if (participants <= 0 || participants > session.getSpotsLeft()) {
            throw new IllegalArgumentException(
                    "Invalid seeded booking: " + participants + " participants, " + session.getSpotsLeft() + " spots left."
            );
        }

        BigDecimal subtotal = session.getActivity()
                .getPricePerPerson()
                .multiply(BigDecimal.valueOf(participants));

        Bookings booking = new Bookings();
        booking.setUser(customer);
        booking.setSession(session);
        booking.setParticipants(participants);
        booking.setBookingType(participants > 1 ? BookingType.GROUP : BookingType.INDIVIDUAL);
        booking.setSubtotal(subtotal);
        booking.setDiscountPercent(BigDecimal.ZERO);
        booking.setTotalPrice(subtotal);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        booking.setCreatedBy(customer.getEmail());
        booking.setUpdatedBy(customer.getEmail());

        Bookings saved = bookingsRepository.save(booking);

        session.setSpotsLeft(session.getSpotsLeft() - participants);
        if (session.getSpotsLeft() == 0) session.setStatus(SessionStatus.FULL);
        sessionsRepository.save(session);

        return saved;
    }
}