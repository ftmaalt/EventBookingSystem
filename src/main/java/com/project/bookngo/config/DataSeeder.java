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
import java.util.Objects;

/**
 * Adds repeatable Project 3 presentation data without resetting the development database.
 * Existing sessions and bookings are never rewritten, including sessions already used in JMeter.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsersRepository usersRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderApplicationRepository providerApplicationRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ActivitiesRepository activitiesRepository;
    private final SessionsRepository sessionsRepository;
    private final BookingsRepository bookingsRepository;
    private final WaitingListRepository waitingListRepository;
    private final PasswordEncoder passwordEncoder;

    /** Only used when creating new development accounts. Never print the password. */
    @Value("${bookngo.demo.password:BookNGo123!}")
    private String demoPassword;

    private record DemoSession(String label, Sessions session, boolean created) {}

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== BookNGo Project 3 demo seeding ==========");

        // Existing project accounts: reuse them if they already exist.
        User admin = user("Admin User", "admin@bookngo.com", "30000001", UserRole.ADMIN, UserStatus.ACTIVE);
        User layla = user("Layla Ahmed", "provider@bookngo.com", "30000002", UserRole.PROVIDER, UserStatus.ACTIVE);
        User omar = user("Omar Hassan", "provider2@bookngo.com", "30000003", UserRole.PROVIDER, UserStatus.ACTIVE);
        User sara = user("Sara Mohamed", "customer@bookngo.com", "30000004", UserRole.USER, UserStatus.ACTIVE);
        User ali = user("Ali Ahmed", "customer2@bookngo.com", "30000005", UserRole.USER, UserStatus.ACTIVE);
        user("Noora Khalid", "customer3@bookngo.com", "30000006", UserRole.USER, UserStatus.DEACTIVATED);

        // Fourteen additional customers and one additional provider.
        User jmeterA = user("JMeter Customer A", "jmeter1@bookngo.com", "30000101", UserRole.USER, UserStatus.ACTIVE);
        User jmeterB = user("JMeter Customer B", "jmeter2@bookngo.com", "30000102", UserRole.USER, UserStatus.ACTIVE);
        User wait1 = user("Waitlist Customer One", "waitlist1@bookngo.com", "30000103", UserRole.USER, UserStatus.ACTIVE);
        User wait2 = user("Waitlist Customer Two", "waitlist2@bookngo.com", "30000104", UserRole.USER, UserStatus.ACTIVE);
        User wait3 = user("Waitlist Customer Three", "waitlist3@bookngo.com", "30000105", UserRole.USER, UserStatus.ACTIVE);
        User booking1 = user("Booking Customer One", "booking1@bookngo.com", "30000106", UserRole.USER, UserStatus.ACTIVE);
        User booking2 = user("Booking Customer Two", "booking2@bookngo.com", "30000107", UserRole.USER, UserStatus.ACTIVE);
        User booking3 = user("Booking Customer Three", "booking3@bookngo.com", "30000108", UserRole.USER, UserStatus.ACTIVE);
        User cancel1 = user("Cancellation Customer One", "cancel1@bookngo.com", "30000109", UserRole.USER, UserStatus.ACTIVE);
        User cancel2 = user("Cancellation Customer Two", "cancel2@bookngo.com", "30000110", UserRole.USER, UserStatus.ACTIVE);
        User demo1 = user("Demo Customer One", "demo1@bookngo.com", "30000111", UserRole.USER, UserStatus.ACTIVE);
        User demo2 = user("Demo Customer Two", "demo2@bookngo.com", "30000112", UserRole.USER, UserStatus.ACTIVE);
        User notify = user("Notification Customer", "notify1@bookngo.com", "30000113", UserRole.USER, UserStatus.ACTIVE);
        user("Inactive Demo Customer", "inactive1@bookngo.com", "30000114", UserRole.USER, UserStatus.DEACTIVATED);
        User provider3 = user("Mariam Saleh", "provider3@bookngo.com", "30000115", UserRole.PROVIDER, UserStatus.ACTIVE);

        profile(layla, "Layla Experiences", "Creative workshops and guided experiences.", "30000002");
        profile(omar, "Omar Adventures", "Outdoor activities and adventure experiences.", "30000003");
        profile(provider3, "Mariam Events", "Art and lifestyle experiences in Bahrain.", "30000115");
        application(sara, admin, "Sara Creative Studio", ApplicationStatus.PENDING);
        application(omar, admin, "Omar Adventures", ApplicationStatus.APPROVED);

        Category outdoor = category("Outdoor Adventures", "Kayaking, camping, and outdoor experiences.");
        Category creative = category("Creative Workshops", "Pottery, painting, and creative classes.");
        Category fitness = category("Fitness & Wellness", "Yoga, fitness, and wellness activities.");
        Category food = category("Food & Cooking", "Cooking classes and culinary experiences.");

        // Seed only missing locations. NEVER reset a provider's edited map pin or Google Place ID.
        Location manama = location("BookNGo Creative Studio", "Exhibition Road", "Manama",
                26.242552, 50.553782, layla);
        Location amwaj = location("Amwaj Adventure Center", "Amwaj Islands", "Muharraq",
                26.2870, 50.6640, omar);
        Location riffa = location("Riffa Activity Center", "Riffa", "Southern Governorate",
                26.1300, 50.5550, layla);
        Location mariamStudio = location("Mariam Art Studio", "Manama", "Manama",
                26.2235, 50.5876, provider3);

        Activities kayaking = activity("Sunset Kayaking Tour", "Guided kayaking along the coast.",
                "15.000", 90, outdoor, amwaj, omar);
        Activities pottery = activity("Beginner Pottery Workshop", "Learn pottery techniques with an instructor.",
                "20.000", 120, creative, manama, layla);
        Activities yoga = activity("Morning Yoga Session", "Relaxing beginner-friendly yoga class.",
                "8.000", 60, fitness, riffa, layla);
        Activities cooking = activity("Traditional Bahraini Cooking", "Learn traditional Bahraini recipes.",
                "25.000", 150, food, manama, layla);
        Activities watercolor = activity("Watercolor Painting Evening", "Relaxed guided watercolor painting.",
                "12.000", 90, creative, mariamStudio, provider3);
        Activities wellness = activity("Weekend Mobility Workshop", "Beginner mobility and stretching session.",
                "10.000", 75, fitness, riffa, layla);

        // GENERAL BROWSING: a mix of empty, partially booked and full sessions.
        DemoSession browse1 = session("BROWSE_KAYAKING", kayaking, at(2026, 11, 5, 17), 8);
        DemoSession browse2 = session("BROWSE_POTTERY_FULL", pottery, at(2026, 11, 6, 15), 4);
        DemoSession browse3 = session("BROWSE_YOGA", yoga, at(2026, 11, 7, 9), 12);
        DemoSession browse4 = session("BROWSE_COOKING", cooking, at(2026, 11, 9, 14), 6);
        DemoSession browse5 = session("BROWSE_WATERCOLOR", watercolor, at(2026, 11, 10, 18), 10);
        DemoSession browse6 = session("BROWSE_MOBILITY", wellness, at(2026, 11, 23, 10), 8);
        if (browse2.created()) {
            booking(sara, browse2.session(), 2);
            booking(ali, browse2.session(), 2);
        }
        if (browse4.created()) {
            booking(demo1, browse4.session(), 2);
            booking(demo2, browse4.session(), 1);
        }
        if (browse5.created()) booking(booking3, browse5.session(), 2);

        // JMETER RACE: three untouched sessions, exactly one unreserved seat each.
        Activities race = activity("DEMO - Last Seat Race Condition", "JMeter presentation scenario.",
                "10.000", 60, creative, manama, layla);
        DemoSession racePractice = session("JMETER_RACE_01_PRACTICE", race, at(2026, 11, 12, 17), 1);
        DemoSession raceLive = session("JMETER_RACE_02_PRESENTATION", race, at(2026, 11, 17, 17), 1);
        DemoSession raceBackup = session("JMETER_RACE_03_BACKUP", race, at(2026, 11, 22, 17), 1);

        // FIFO: cancelling the original booking allocates the seat to waitlist1 first.
        Activities fifo = activity("DEMO - FIFO Waitlist", "Single seat with three waiting customers.",
                "18.000", 90, creative, manama, layla);
        DemoSession fifoDemo = session("WAITLIST_FIFO", fifo, at(2026, 11, 14, 16), 1);
        if (fifoDemo.created()) {
            booking(booking1, fifoDemo.session(), 1);
            waitlist(wait1, fifoDemo.session());
            waitlist(wait2, fifoDemo.session());
            waitlist(wait3, fifoDemo.session());
        }

        // CONCURRENT CANCELLATIONS: two bookings release two seats to two waiting users.
        Activities cancellation = activity("DEMO - Concurrent Cancellation", "Two bookings and two waiting customers.",
                "14.000", 60, outdoor, amwaj, omar);
        DemoSession concurrentCancel = session("CONCURRENT_CANCELLATIONS", cancellation,
                at(2026, 11, 15, 17), 2);
        if (concurrentCancel.created()) {
            booking(cancel1, concurrentCancel.session(), 1);
            booking(cancel2, concurrentCancel.session(), 1);
            waitlist(wait1, concurrentCancel.session());
            waitlist(wait2, concurrentCancel.session());
        }

        // DUPLICATE CANCELLATION: attempt to cancel this same booking twice concurrently.
        Activities duplicate = activity("DEMO - Duplicate Cancellation", "One booking, two cancellation attempts.",
                "16.000", 60, fitness, riffa, layla);
        DemoSession duplicateCancel = session("DUPLICATE_CANCELLATION", duplicate,
                at(2026, 11, 16, 11), 1);
        if (duplicateCancel.created()) booking(booking2, duplicateCancel.session(), 1);

        // CAPACITY: four of five spots booked, leaving one.
        Activities capacity = activity("DEMO - Capacity Validation", "One seat remains; a group of two cannot book.",
                "11.000", 75, food, manama, layla);
        DemoSession capacityDemo = session("CAPACITY_VALIDATION", capacity,
                at(2026, 11, 18, 17), 5);
        if (capacityDemo.created()) {
            booking(booking1, capacityDemo.session(), 2);
            booking(booking2, capacityDemo.session(), 2);
        }

        // ASYNC: leave available so the live booking publishes email/SSE events after commit.
        Activities async = activity("DEMO - Async Notifications", "Create a booking to trigger async notifications.",
                "9.000", 90, creative, manama, layla);
        DemoSession asyncDemo = session("ASYNC_NOTIFICATIONS", async,
                at(2026, 11, 20, 18), 4);

        log.info("---------- Project 3 scenario IDs (do not assume IDs are fixed) ----------");
        show(racePractice);
        show(raceLive);
        show(raceBackup);
        show(fifoDemo);
        show(concurrentCancel);
        show(duplicateCancel);
        show(capacityDemo);
        show(asyncDemo);
        show(browse1);
        show(browse2);
        show(browse3);
        show(browse4);
        show(browse5);
        show(browse6);
        log.info("JMeter customers: {} and {}", jmeterA.getEmail(), jmeterB.getEmail());
        log.info("Waitlist FIFO: {} -> {} -> {}", wait1.getEmail(), wait2.getEmail(), wait3.getEmail());
        log.info("Concurrent cancellation booking owners: {}, {}", cancel1.getEmail(), cancel2.getEmail());
        log.info("Async demo booking customer: {}", notify.getEmail());
        log.info("Demo browsing customers: {}, {}", demo1.getEmail(), demo2.getEmail());
        log.info("Users={}, Activities={}, Sessions={}, Bookings={}, Waiting={}",
                usersRepository.count(), activitiesRepository.count(), sessionsRepository.count(),
                bookingsRepository.count(), waitingListRepository.count());
        log.info("Existing scenarios are NOT reset on restart. Create a new session after consuming all backups.");
        log.info("=============================================================");
    }

    private void show(DemoSession demo) {
        Sessions s = demo.session();
        log.info("{} | sessionId={} | {} | capacity={} | spotsLeft={} | status={} | {}",
                demo.label(), s.getId(), s.getStartTime(), s.getCapacity(), s.getSpotsLeft(),
                s.getStatus(), demo.created() ? "NEW" : "EXISTING - UNCHANGED");
    }

    private static LocalDateTime at(int year, int month, int day, int hour) {
        return LocalDateTime.of(year, month, day, hour, 0);
    }

    private User user(String name, String email, String phone, UserRole role, UserStatus status) {
        User existing = usersRepository.findUserByEmail(email);
        if (existing != null) {
            if (existing.getRole() != role || existing.getStatus() != status) {
                log.warn("Existing account {} has role/status {}/{} (expected {}/{}). Preserving it.",
                        email, existing.getRole(), existing.getStatus(), role, status);
            }
            return existing;
        }
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode(demoPassword));
        u.setRole(role);
        u.setStatus(status);
        u.setMustChangePassword(false);
        u.setStrikeCount((short) 0);
        u.setConsecutiveViolations((short) 0);
        return usersRepository.save(u);
    }

    private void profile(User provider, String name, String description, String phone) {
        if (providerProfileRepository.findByUserId(provider.getId()).isPresent()) return;
        ProviderProfile p = new ProviderProfile();
        p.setUser(provider);
        p.setBusinessName(name);
        p.setDescription(description);
        p.setPhone(phone);
        providerProfileRepository.save(p);
    }

    private void application(User applicant, User admin, String name, ApplicationStatus status) {
        boolean exists = providerApplicationRepository.findByCreatedUserId(applicant.getId()).stream()
                .anyMatch(a -> name.equals(a.getBusinessName()));
        if (exists) return;
        ProviderApplication a = new ProviderApplication();
        a.setCreatedUser(applicant);
        a.setBusinessName(name);
        a.setContactName(applicant.getFullName());
        a.setEmail(applicant.getEmail());
        a.setPhone(applicant.getPhone());
        a.setCity("Manama");
        a.setDescription("Development demonstration application.");
        a.setProposedActivities("Workshops, activities, and experiences.");
        a.setStatus(status);
        if (status == ApplicationStatus.APPROVED) {
            a.setReviewedBy(admin);
            a.setReviewedAt(LocalDateTime.now());
            a.setReviewNote("Approved for development demonstration.");
        }
        providerApplicationRepository.save(a);
    }

    private Category category(String name, String description) {
        Category existing = categoryRepository.findByCategoryName(name);
        if (existing != null) return existing;
        Category c = new Category();
        c.setCategoryName(name);
        c.setDescription(description);
        c.setActive(true);
        return categoryRepository.save(c);
    }

    private Location location(String name, String address, String city,
                              double latitude, double longitude, User provider) {
        return locationRepository.findAll().stream()
                .filter(l -> name.equals(l.getName()) &&
                        provider.getId().equals(l.getProvider().getId()))
                .findFirst()
                .orElseGet(() -> {
                    Location l = new Location();
                    l.setName(name);
                    l.setAddress(address);
                    l.setCity(city);
                    l.setLatitude(latitude);
                    l.setLongitude(longitude);
                    l.setActive(true);
                    l.setProvider(provider);
                    // Do not fabricate a Google Place ID.
                    return locationRepository.save(l);
                });
    }

    private Activities activity(String title, String description, String price,
                                int durationMinutes, Category category, Location location, User provider) {
        return activitiesRepository.findByProviderId(provider.getId()).stream()
                .filter(a -> title.equals(a.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Activities a = new Activities();
                    a.setTitle(title);
                    a.setDescription(description);
                    a.setPricePerPerson(new BigDecimal(price));
                    a.setDurationMinutes(durationMinutes);
                    a.setCategory(category);
                    a.setLocation(location);
                    a.setProvider(provider);
                    a.setStatus(ActivityStatus.ACTIVE);
                    a.setCreatedBy(provider.getEmail());
                    a.setUpdatedBy(provider.getEmail());
                    return activitiesRepository.save(a);
                });
    }

    private DemoSession session(String label, Activities activity,
                                LocalDateTime start, int capacity) {
        Sessions existing = sessionsRepository.findByActivityId(activity.getId()).stream()
                .filter(s -> start.equals(s.getStartTime()))
                .findFirst().orElse(null);
        if (existing != null) return new DemoSession(label, existing, false);

        Sessions s = new Sessions();
        s.setActivity(activity);
        s.setStartTime(start);
        s.setEndTime(start.plusMinutes(activity.getDurationMinutes()));
        s.setCapacity(capacity);
        s.setSpotsLeft(capacity);
        s.setStatus(SessionStatus.SCHEDULED);
        return new DemoSession(label, sessionsRepository.save(s), true);
    }

    private Bookings booking(User customer, Sessions session, int participants) {
        if (bookingsRepository.findBySessionId(session.getId()).stream()
                .anyMatch(b -> customer.getId().equals(b.getUser().getId()) &&
                        b.getStatus() != BookingStatus.CANCELLED)) {
            throw new IllegalStateException("Demo user already booked session " + session.getId());
        }
        if (participants <= 0 || participants > session.getSpotsLeft()) {
            throw new IllegalArgumentException("Invalid participants for seeded session " + session.getId());
        }
        BigDecimal price = session.getActivity().getPricePerPerson()
                .multiply(BigDecimal.valueOf(participants));
        Bookings b = new Bookings();
        b.setUser(customer);
        b.setSession(session);
        b.setParticipants(participants);
        b.setBookingType(participants > 1 ? BookingType.GROUP : BookingType.INDIVIDUAL);
        b.setSubtotal(price);
        b.setDiscountPercent(BigDecimal.ZERO);
        b.setTotalPrice(price);
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        b.setCreatedBy(customer.getEmail());
        b.setUpdatedBy(customer.getEmail());
        Bookings saved = bookingsRepository.save(b);

        session.setSpotsLeft(session.getSpotsLeft() - participants);
        session.setStatus(session.getSpotsLeft() == 0 ? SessionStatus.FULL : SessionStatus.SCHEDULED);
        sessionsRepository.save(session);
        return saved;
    }

    private WaitlistEntry waitlist(User customer, Sessions session) {
        boolean alreadyPresent = waitingListRepository.findAll().stream()
                .anyMatch(e -> Objects.equals(e.getSession().getId(), session.getId()) &&
                        Objects.equals(e.getUser().getId(), customer.getId()));
        if (alreadyPresent) throw new IllegalStateException("Duplicate demo waitlist entry");
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(customer);
        entry.setSession(session);
        entry.setStatus(WaitListStatus.WAITING);
        // IDENTITY key assignment preserves FIFO when ordered by entryId.
        return waitingListRepository.saveAndFlush(entry);
    }
}
