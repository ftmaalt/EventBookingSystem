package com.bookngo.authservice;

import com.bookngo.authservice.config.DemoDataSeeder;
import com.bookngo.authservice.model.ProviderApplication;
import com.bookngo.authservice.model.ProviderProfile;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.ApplicationStatus;
import com.bookngo.authservice.model.enums.UserRole;
import com.bookngo.authservice.model.enums.UserStatus;
import com.bookngo.authservice.repository.ProviderApplicationRepository;
import com.bookngo.authservice.repository.ProviderProfileRepository;
import com.bookngo.authservice.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DemoDataSeederTest {

    private UsersRepository usersRepository;
    private ProviderProfileRepository profileRepository;
    private ProviderApplicationRepository applicationRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usersRepository = mock(UsersRepository.class);
        profileRepository = mock(ProviderProfileRepository.class);
        applicationRepository = mock(ProviderApplicationRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(usersRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    private DemoDataSeeder seeder(String password) {
        return new DemoDataSeeder(usersRepository, profileRepository, applicationRepository, passwordEncoder, password);
    }

    @Test
    void createsTheDemoUsersOnAnEmptyDatabase() {
        when(usersRepository.count()).thenReturn(0L);

        seeder("demo-password").run(null);

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(usersRepository, times(6)).save(users.capture());
        Map<String, UserRole> roles = users.getAllValues().stream()
                .collect(Collectors.toMap(User::getEmail, User::getRole));
        assertEquals(Map.of(
                "admin@bookngo.demo", UserRole.ADMIN,
                "provider1@bookngo.demo", UserRole.PROVIDER,
                "provider2@bookngo.demo", UserRole.PROVIDER,
                "customer1@bookngo.demo", UserRole.USER,
                "customer2@bookngo.demo", UserRole.USER,
                "customer3@bookngo.demo", UserRole.USER), roles);
        users.getAllValues().forEach(user -> {
            assertEquals(UserStatus.ACTIVE, user.getStatus());
            assertEquals("encoded", user.getPasswordHash());
            assertTrue(user.getPhone().matches("\\d{8}"));
        });
        verify(passwordEncoder, times(6)).encode("demo-password");

        ArgumentCaptor<ProviderApplication> applications = ArgumentCaptor.forClass(ProviderApplication.class);
        verify(applicationRepository, times(2)).save(applications.capture());
        applications.getAllValues().forEach(application -> {
            assertEquals(ApplicationStatus.APPROVED, application.getStatus());
            assertEquals("admin@bookngo.demo", application.getReviewedBy().getEmail());
            assertNotNull(application.getReviewedAt());
        });

        ArgumentCaptor<ProviderProfile> profiles = ArgumentCaptor.forClass(ProviderProfile.class);
        verify(profileRepository, times(2)).save(profiles.capture());
        List<String> businesses = profiles.getAllValues().stream().map(ProviderProfile::getBusinessName).toList();
        assertEquals(List.of("Amwaj Sea Kayaking", "Manama Clay Studio"), businesses);
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        when(usersRepository.count()).thenReturn(6L);

        seeder("demo-password").run(null);

        verify(usersRepository, never()).save(any());
        verifyNoInteractions(profileRepository, applicationRepository, passwordEncoder);
    }

    @Test
    void failsWhenThePasswordIsBlank() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> seeder(" ").run(null));

        assertTrue(error.getMessage().contains("SEED_PASSWORD"));
        verify(usersRepository, never()).save(any());
    }

    @Test
    void failsWhenThePasswordIsTooShortToLogIn() {
        assertThrows(IllegalStateException.class, () -> seeder("short").run(null));

        verify(usersRepository, never()).save(any());
    }
}
