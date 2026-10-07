package com.project.bookngo.service;

import com.project.bookngo.exception.InformationExistsException;
import com.project.bookngo.exception.InvalidCredentials;
import com.project.bookngo.exception.VerificationRequiredException;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.enums.UserStatus;
import com.project.bookngo.model.request.LoginRequest;
import com.project.bookngo.model.request.RegisterRequest;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterNewUser() {
        String email = "testuser" + System.currentTimeMillis() + "@example.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFullname("Test User");
        request.setPhone("39999999");
        request.setPassword("Password123!");

        GenericMessageResponse response = authService.register(request);

        assertNotNull(response);

        User user = usersRepository.findUserByEmail(email);

        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertEquals("Test User", user.getFullName());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
    }


    @Test
    @DisplayName("Should reject registration when email already exists")
    void shouldNotRegisterUserWithExistingEmail() {
        String email = "existing" + System.currentTimeMillis() + "@example.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFullname("Existing User");
        request.setPhone("39999999");
        request.setPassword("Password123!");

        authService.register(request);

        assertThrows(
                InformationExistsException.class,
                () -> authService.register(request)
        );
    }


    @Test
    @DisplayName("Should hash the user's password during registration")
    void shouldHashUserPassword() {
        String email = "passwordtest" + System.currentTimeMillis() + "@example.com";
        String rawPassword = "Password123!";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFullname("Password Test");
        request.setPhone("39999999");
        request.setPassword(rawPassword);

        authService.register(request);

        User user = usersRepository.findUserByEmail(email);

        assertNotNull(user);
        assertNotEquals(rawPassword, user.getPasswordHash());
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }


    @Test
    @DisplayName("Should prevent an unverified user from logging in")
    void shouldNotAllowUnverifiedUserToLogin() {
        String email = "unverified" + System.currentTimeMillis() + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setFullname("Unverified User");
        registerRequest.setPhone("39999999");
        registerRequest.setPassword("Password123!");

        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("Password123!");

        assertThrows(
                VerificationRequiredException.class,
                () -> authService.login(loginRequest)
        );
    }


    @Test
    @DisplayName("Should reject login when the password is incorrect")
    void shouldNotAllowLoginWithIncorrectPassword() {
        String email = "wrongpassword" + System.currentTimeMillis() + "@example.com";

        User user = new User();
        user.setEmail(email);
        user.setFullName("Password Test");
        user.setPhone("39999999");
        user.setPasswordHash(passwordEncoder.encode("CorrectPassword123!"));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setStrikeCount((short) 0);
        user.setConsecutiveViolations((short) 0);
        user.setMustChangePassword(false);

        usersRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("WrongPassword123!");

        assertThrows(
                InvalidCredentials.class,
                () -> authService.login(loginRequest)
        );
    }
}