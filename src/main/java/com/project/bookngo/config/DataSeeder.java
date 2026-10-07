package com.project.bookngo.config;

import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ActivitiesRepository activitiesRepository;

    @Autowired
    private SessionsRepository sessionsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) {
        if (usersRepository.count() > 0) {
            return; // don't duplicate on every restart
        }

        // --- Users ---
        User admin = new User();
        admin.setFullName("Admin User");
        admin.setEmail("admin@bookngo.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
        admin.setPhone("00000000");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setMustChangePassword(false);
        admin.setStrikeCount((short) 0);
        admin.setConsecutiveViolations((short) 0);
        admin = usersRepository.save(admin);

        User provider = new User();
        provider.setFullName("Layla Provider");
        provider.setEmail("provider@bookngo.com");
        provider.setPasswordHash(passwordEncoder.encode("Provider123!"));
        provider.setPhone("11111111");
        provider.setRole(UserRole.PROVIDER);
        provider.setStatus(UserStatus.ACTIVE);
        provider.setMustChangePassword(false);
        provider.setStrikeCount((short) 0);
        provider.setConsecutiveViolations((short) 0);
        provider = usersRepository.save(provider);

        User customer = new User();
        customer.setFullName("Sara Customer");
        customer.setEmail("customer@bookngo.com");
        customer.setPasswordHash(passwordEncoder.encode("Customer123!"));
        customer.setPhone("22222222");
        customer.setRole(UserRole.USER);
        customer.setStatus(UserStatus.ACTIVE);
        customer.setMustChangePassword(false);
        customer.setStrikeCount((short) 0);
        customer.setConsecutiveViolations((short) 0);
        usersRepository.save(customer);

        // --- Categories ---
        Category outdoor = new Category();
        outdoor.setCategoryName("Outdoor Adventures");
        outdoor.setDescription("Kayaking, desert trips, and more.");
        outdoor = categoryRepository.save(outdoor);

        Category creative = new Category();
        creative.setCategoryName("Creative Workshops");
        creative.setDescription("Pottery, painting, and hands-on classes.");
        creative = categoryRepository.save(creative);

        // --- Location ---
        Location location = new Location();
        location.setName("BookNGo Studio - Manama");
        location.setAddress("Building 123, Road 456");
        location.setCity("Manama");
        location.setProvider(provider);
        location.setLatitude(26.2235);
        location.setLongitude(50.5876);
        location.setActive(true);
        location = locationRepository.save(location);

        // --- Activity ---
        Activities kayaking = new Activities();
        kayaking.setTitle("Sunset Kayaking Tour");
        kayaking.setDescription("A guided kayaking tour along the coast at sunset.");
        kayaking.setPricePerPerson(new java.math.BigDecimal("15.000"));
        kayaking.setDurationMinutes(90);
        kayaking.setCategory(outdoor);
        kayaking.setLocation(location);
        kayaking.setProvider(provider);
        kayaking.setStatus(ActivityStatus.ACTIVE);
        kayaking.setCreatedBy(provider.getEmail());
        kayaking.setUpdatedBy(provider.getEmail());
        kayaking = activitiesRepository.save(kayaking);

        Activities pottery = new Activities();
        pottery.setTitle("Beginner Pottery Class");
        pottery.setDescription("Learn the basics of pottery in a relaxed studio setting.");
        pottery.setPricePerPerson(new java.math.BigDecimal("20.000"));
        pottery.setDurationMinutes(120);
        pottery.setCategory(creative);
        pottery.setLocation(location);
        pottery.setProvider(provider);
        pottery.setStatus(ActivityStatus.ACTIVE);
        pottery.setCreatedBy(provider.getEmail());
        pottery.setUpdatedBy(provider.getEmail());
        activitiesRepository.save(pottery);

        // --- Sessions for kayaking ---
        Sessions session1 = new Sessions();
        session1.setActivity(kayaking);
        session1.setStartTime(LocalDateTime.now().plusDays(2).withHour(17).withMinute(0));
        session1.setEndTime(LocalDateTime.now().plusDays(2).withHour(18).withMinute(30));
        session1.setCapacity(8);
        session1.setSpotsLeft(8);
        session1.setStatus(SessionStatus.SCHEDULED);
        sessionsRepository.save(session1);

        Sessions session2 = new Sessions();
        session2.setActivity(kayaking);
        session2.setStartTime(LocalDateTime.now().plusDays(5).withHour(17).withMinute(0));
        session2.setEndTime(LocalDateTime.now().plusDays(5).withHour(18).withMinute(30));
        session2.setCapacity(8);
        session2.setSpotsLeft(8);
        session2.setStatus(SessionStatus.SCHEDULED);
        sessionsRepository.save(session2);

        System.out.println("=== Seed data loaded: admin@bookngo.com / provider@bookngo.com / customer@bookngo.com (all status ACTIVE, passwords as coded above) ===");
    }
}