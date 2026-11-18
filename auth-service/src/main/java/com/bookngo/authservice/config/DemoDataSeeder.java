package com.bookngo.authservice.config;

import com.bookngo.authservice.model.ProviderApplication;
import com.bookngo.authservice.model.ProviderProfile;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.ApplicationStatus;
import com.bookngo.authservice.model.enums.UserRole;
import com.bookngo.authservice.model.enums.UserStatus;
import com.bookngo.authservice.repository.ProviderApplicationRepository;
import com.bookngo.authservice.repository.ProviderProfileRepository;
import com.bookngo.authservice.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UsersRepository usersRepository;
    private final ProviderProfileRepository profileRepository;
    private final ProviderApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DemoDataSeeder(UsersRepository usersRepository, ProviderProfileRepository profileRepository,
                          ProviderApplicationRepository applicationRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.seed.password:}") String seedPassword) {
        this.usersRepository = usersRepository;
        this.profileRepository = profileRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException("SEED_ENABLED is true but SEED_PASSWORD is empty. Set SEED_PASSWORD or set SEED_ENABLED=false.");
        }
        if (seedPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("SEED_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters, otherwise the demo accounts cannot log in.");
        }
        if (usersRepository.count() > 0) {
            logger.info("Demo seed skipped: users already exist.");
            return;
        }

        User admin = createUser("Hamad Al-Sayed", "admin@bookngo.demo", "36001122", UserRole.ADMIN);

        User kayakProvider = createUser("Yusuf Al-Jishi", "provider1@bookngo.demo", "33445566", UserRole.PROVIDER);
        approveProvider(kayakProvider, admin, "Amwaj Sea Kayaking", "Muharraq",
                "Guided sea kayaking and paddleboarding tours around the Amwaj Islands lagoons.",
                "Sunset kayaking tours, beginner paddleboarding lessons");

        User potteryProvider = createUser("Maryam Al-Aradi", "provider2@bookngo.demo", "39887766", UserRole.PROVIDER);
        approveProvider(potteryProvider, admin, "Manama Clay Studio", "Manama",
                "Hands-on pottery and ceramics workshops in a small studio in Adliya.",
                "Wheel-throwing workshops, hand-building classes for families");

        createUser("Ali Hasan", "customer1@bookngo.demo", "34112233", UserRole.USER);
        createUser("Zainab Abdulla", "customer2@bookngo.demo", "35223344", UserRole.USER);
        createUser("Ahmed Janahi", "customer3@bookngo.demo", "36334455", UserRole.USER);
    }

    private User createUser(String fullName, String email, String phone, UserRole role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        User saved = usersRepository.save(user);
        logger.info("Seeded {} user {}", role, email);
        return saved;
    }

    private void approveProvider(User provider, User admin, String businessName, String city,
                                 String description, String proposedActivities) {
        ProviderApplication application = new ProviderApplication();
        application.setBusinessName(businessName);
        application.setContactName(provider.getFullName());
        application.setEmail(provider.getEmail());
        application.setPhone(provider.getPhone());
        application.setCity(city);
        application.setDescription(description);
        application.setProposedActivities(proposedActivities);
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewNote("Approved during demo setup.");
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);
        application.setCreatedUser(provider);
        applicationRepository.save(application);

        ProviderProfile profile = new ProviderProfile();
        profile.setBusinessName(businessName);
        profile.setDescription(description);
        profile.setPhone(provider.getPhone());
        profile.setUser(provider);
        profileRepository.save(profile);
    }
}
