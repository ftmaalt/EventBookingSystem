package com.project.bookngo.config;

import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.repository.*;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UsersRepository usersRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ActivitiesRepository activitiesRepository;
    private final SessionsRepository sessionsRepository;
    private final BookingsRepository bookingsRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderApplicationRepository providerApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "BookNGo123!";

    @Override
    @Transactional
    public void run(String... args) {

        if (usersRepository.count() > 0) {
            logger.info("Database contains users; skipping demo seeding.");
            return;
        }

        logger.info("Starting BookNGo development data seeding.");


        User admin = createUser(
                "Admin User", "admin@bookngo.com",
                "30000001", UserRole.ADMIN, UserStatus.ACTIVE
        );

        User provider1 = createUser(
                "Layla Ahmed", "provider@bookngo.com",
                "30000002", UserRole.PROVIDER, UserStatus.ACTIVE
        );

        User provider2 = createUser(
                "Omar Hassan", "provider2@bookngo.com",
                "30000003", UserRole.PROVIDER, UserStatus.ACTIVE
        );

        User customer1 = createUser(
                "Sara Mohamed", "customer@bookngo.com",
                "30000004", UserRole.USER, UserStatus.ACTIVE
        );

        User customer2 = createUser(
                "Ali Ahmed", "customer2@bookngo.com",
                "30000005", UserRole.USER, UserStatus.ACTIVE
        );

        User customer3 = createUser(
                "Noora Khalid", "customer3@bookngo.com",
                "30000006", UserRole.USER, UserStatus.DEACTIVATED
        );

        logger.info("Users seeded successfully.");

        createProviderProfile(
                provider1,
                "Layla Experiences",
                "Creative workshops and guided experiences.",
                "30000002"
        );

        createProviderProfile(
                provider2,
                "Omar Adventures",
                "Outdoor activities and adventure experiences.",
                "30000003"
        );

        // ========================================
        // 3. PROVIDER APPLICATIONS
        // ========================================

        createProviderApplication(
                customer1, admin,
                "Sara Creative Studio",
                ApplicationStatus.PENDING
        );

        createProviderApplication(
                provider2, admin,
                "Omar Adventures",
                ApplicationStatus.APPROVED
        );

        Category outdoor = createCategory(
                "Outdoor Adventures",
                "Kayaking, camping, and outdoor experiences."
        );

        Category creative = createCategory(
                "Creative Workshops",
                "Pottery, painting, and creative classes."
        );

        Category fitness = createCategory(
                "Fitness & Wellness",
                "Yoga, fitness, and wellness activities."
        );

        Category food = createCategory(
                "Food & Cooking",
                "Cooking classes and culinary experiences."
        );

        // ========================================
        // 5. LOCATIONS
        // ========================================

        Location manama = createLocation(
                "BookNGo Creative Studio",
                "Exhibition Road",
                "Manama",
                26.2285,
                50.5860,
                provider1
        );

        Location amwaj = createLocation(
                "Amwaj Adventure Center",
                "Amwaj Islands",
                "Muharraq",
                26.2870,
                50.6640,
                provider2
        );

        Location riffa = createLocation(
                "Riffa Activity Center",
                "Riffa",
                "Southern Governorate",
                26.1300,
                50.5550,
                provider1
        );


        Activities kayaking = createActivity(
                "Sunset Kayaking Tour",
                "Guided kayaking along the coast.",
                "15.000", 90,
                outdoor, amwaj, provider2
        );

        Activities pottery = createActivity(
                "Beginner Pottery Workshop",
                "Learn pottery techniques with an instructor.",
                "20.000", 120,
                creative, manama, provider1
        );

        Activities yoga = createActivity(
                "Morning Yoga Session",
                "Relaxing beginner-friendly yoga class.",
                "8.000", 60,
                fitness, riffa, provider1
        );

        Activities cooking = createActivity(
                "Traditional Bahraini Cooking",
                "Learn to prepare traditional Bahraini dishes.",
                "25.000", 150,
                food, manama, provider1
        );


        Sessions kayakingAvailable = createSession(
                kayaking, 2, 17, 8
        );


        Sessions kayakingLimited = createSession(
                kayaking, 5, 17, 2
        );

        Sessions potteryFull = createSession(
                pottery, 3, 15, 4
        );

        // Available: 10/10 spots
        Sessions potteryAvailable = createSession(
                pottery, 7, 15, 10
        );

        Sessions yogaAvailable = createSession(
                yoga, 4, 9, 12
        );


        Sessions cookingAvailable = createSession(
                cooking, 6, 14, 6
        );

        createBooking(customer1, kayakingLimited, 1);


        createBooking(customer1, potteryFull, 2);
        createBooking(customer2, potteryFull, 2);

        logger.info("BookNGo development seeding completed.");
        logger.info("Users: {}", usersRepository.count());
        logger.info("Categories: {}", categoryRepository.count());
        logger.info("Activities: {}", activitiesRepository.count());
        logger.info("Sessions: {}", sessionsRepository.count());
        logger.info("Bookings: {}", bookingsRepository.count());
    }


    private User createUser(
            String name,
            String email,
            String phone,
            UserRole role,
            UserStatus status) {

        User user = new User();

        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(
                passwordEncoder.encode(DEMO_PASSWORD)
        );

        user.setRole(role);
        user.setStatus(status);
        user.setMustChangePassword(false);
        user.setStrikeCount((short) 0);
        user.setConsecutiveViolations((short) 0);

        return usersRepository.save(user);
    }

    private ProviderProfile createProviderProfile(
            User provider,
            String businessName,
            String description,
            String phone) {

        ProviderProfile profile = new ProviderProfile();

        profile.setUser(provider);
        profile.setBusinessName(businessName);
        profile.setDescription(description);
        profile.setPhone(phone);

        return providerProfileRepository.save(profile);
    }

    private ProviderApplication createProviderApplication(
            User applicant,
            User admin,
            String businessName,
            ApplicationStatus status) {

        ProviderApplication application =
                new ProviderApplication();

        application.setCreatedUser(applicant);
        application.setBusinessName(businessName);
        application.setContactName(applicant.getFullName());
        application.setEmail(applicant.getEmail());
        application.setPhone(applicant.getPhone());
        application.setCity("Manama");
        application.setDescription(
                "Application to become a BookNGo provider."
        );
        application.setProposedActivities(
                "Workshops, activities, and experiences."
        );

        application.setStatus(status);

        if (status == ApplicationStatus.APPROVED) {
            application.setReviewedBy(admin);
            application.setReviewedAt(LocalDateTime.now());
            application.setReviewNote(
                    "Approved for development demonstration."
            );
        }

        return providerApplicationRepository.save(application);
    }

    private Category createCategory(
            String name,
            String description) {

        Category category = new Category();

        category.setCategoryName(name);
        category.setDescription(description);

        // If your current Category entity has the soft-delete
        // active field, uncomment this:
        // category.setActive(true);

        return categoryRepository.save(category);
    }

    private Location createLocation(
            String name,
            String address,
            String city,
            double latitude,
            double longitude,
            User provider) {

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

    private Activities createActivity(
            String title,
            String description,
            String price,
            int durationMinutes,
            Category category,
            Location location,
            User provider) {

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

    private Sessions createSession(
            Activities activity,
            int daysFromNow,
            int startHour,
            int capacity) {

        Sessions session = new Sessions();

        LocalDateTime start = LocalDateTime.now()
                .plusDays(daysFromNow)
                .toLocalDate()
                .atTime(startHour, 0);

        session.setActivity(activity);
        session.setStartTime(start);
        session.setEndTime(
                start.plusMinutes(activity.getDurationMinutes())
        );

        session.setCapacity(capacity);
        session.setSpotsLeft(capacity);
        session.setStatus(SessionStatus.SCHEDULED);

        return sessionsRepository.save(session);
    }

    private Bookings createBooking(
            User customer,
            Sessions session,
            int participants) {

        if (participants <= 0 ||
                participants > session.getSpotsLeft()) {
            throw new IllegalArgumentException(
                    "Invalid seeded booking participants."
            );
        }

        BigDecimal subtotal = session.getActivity()
                .getPricePerPerson()
                .multiply(BigDecimal.valueOf(participants));

        Bookings booking = new Bookings();

        booking.setUser(customer);
        booking.setSession(session);
        booking.setParticipants(participants);

        booking.setBookingType(
                participants > 1
                        ? BookingType.GROUP
                        : BookingType.INDIVIDUAL
        );

        booking.setSubtotal(subtotal);
        booking.setDiscountPercent(BigDecimal.ZERO);
        booking.setTotalPrice(subtotal);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        booking.setCreatedBy(customer.getEmail());
        booking.setUpdatedBy(customer.getEmail());

        Bookings saved = bookingsRepository.save(booking);

        session.setSpotsLeft(
                session.getSpotsLeft() - participants
        );

        if (session.getSpotsLeft() == 0) {
            session.setStatus(SessionStatus.FULL);
        }

        sessionsRepository.save(session);

        return saved;
    }
}
