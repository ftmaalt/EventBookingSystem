package com.bookngo.authservice.service;

import com.bookngo.authservice.exception.InformationExistsException;
import com.bookngo.authservice.exception.InvalidCredentials;
import com.bookngo.authservice.exception.VerificationRequiredException;

import com.bookngo.authservice.dto.request.*;
import com.bookngo.authservice.event.AccountEvent;
import com.bookngo.authservice.event.AccountEventType;
import com.bookngo.authservice.dto.response.GenericMessageResponse;
import com.bookngo.authservice.dto.response.LoginResponse;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.TokenType;
import com.bookngo.authservice.model.enums.UserRole;
import com.bookngo.authservice.model.enums.UserStatus;
import com.bookngo.authservice.repository.UsersRepository;
import com.bookngo.authservice.security.JwtUtils;
import com.bookngo.authservice.security.MyUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtUtils jwtUtils;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthService(UsersRepository usersRepository, PasswordEncoder passwordEncoder, TokenService tokenService, JwtUtils jwtUtils, CurrentUserService currentUserService, ApplicationEventPublisher eventPublisher) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtUtils = jwtUtils;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }


    // Registration
    @Transactional
    public GenericMessageResponse register(RegisterRequest request) {
        logger.info("User registration request received");
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new InformationExistsException("The email you inputted has already been used. Please try again with another email.");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullname());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        usersRepository.save(user);
        String verificationToken = tokenService.generateToken(user, TokenType.EMAIL_VERIFICATION);
        // published to Kafka after commit; notification-service sends the verification e-mail
        eventPublisher.publishEvent(AccountEvent.withToken(AccountEventType.USER_REGISTERED, user, verificationToken));
        logger.info("User registered successfully with email: {}", user.getEmail());
        return new GenericMessageResponse("Registration successful. Please Verify your account via email to be able to use the app.");
    }

//    Email Verification
    @Transactional
    public GenericMessageResponse verifyEmail(String token) {
        logger.info("Email verification request received");
        User user = tokenService.validateToken(token, TokenType.EMAIL_VERIFICATION);
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException("This account cannot be verified.");
        }
        user.setStatus(UserStatus.ACTIVE);
        usersRepository.save(user);
        logger.info("Email verified successfully for user ID: {}", user.getId());
        return new GenericMessageResponse("Email verification successful, Please log in again to use the app.");
    }

    //  Login
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Login request received");
        User loginAttemptUser = usersRepository.findUserByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login attempt failed: user not found");
                    return new InvalidCredentials("The Email/Password you entered is not correct. Please try again.");
                });

        if (!passwordEncoder.matches(loginRequest.getPassword(), loginAttemptUser.getPasswordHash())) {
            logger.warn("Login attempt failed: incorrect password for user ID: {}", loginAttemptUser.getId());
            throw new InvalidCredentials("The Email/Password you entered is not correct. Please try again.");
        }

        if (loginAttemptUser.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new VerificationRequiredException("Please verify your email before attempting to login.");
        }

        if ((loginAttemptUser.getStatus() == UserStatus.BLACKLISTED)){
            logger.warn("Login attempt blocked for blocked user ID: {} with status: {}", loginAttemptUser.getId(), loginAttemptUser.getStatus());
            throw new InvalidCredentials("Due to violations, your account has been permanently blocked.");
        }

        if (loginAttemptUser.getStatus() == UserStatus.DEACTIVATED) {
            logger.warn("Login attempt blocked for inactive user ID: {} with status: {}", loginAttemptUser.getId(), loginAttemptUser.getStatus());
            throw new InvalidCredentials("This account has been deactivated. Please contact support.");
        }

        String token = jwtUtils.generateJwtToken(new MyUserDetails(loginAttemptUser));
        logger.info("Successful login for user ID: {}", loginAttemptUser.getId());
        return new LoginResponse(token, loginAttemptUser.getRole().name());
    }

//    Forgot Password
    @Transactional
    public GenericMessageResponse forgotPassword(ForgotPasswordRequest request) {
        logger.info("Forgot password request received");
        usersRepository.findUserByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.ACTIVE) {
                String resetToken = tokenService.generateToken(user, TokenType.PASSWORD_RESET);
                eventPublisher.publishEvent(AccountEvent.withToken(AccountEventType.PASSWORD_RESET_REQUESTED, user, resetToken));
            }
        });
        return new GenericMessageResponse("If an eligible account exists, password reset instructions will be sent to its email address.");
    }
//    Reset Password
    @Transactional
    public GenericMessageResponse resetPassword(ResetPasswordRequest passwordRequest){
        logger.info("Password reset request received");
        User user = tokenService.validateToken(passwordRequest.getToken(), TokenType.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(passwordRequest.getNewPassword()));
        usersRepository.save(user);
        return new GenericMessageResponse("Password Reset Successful. You can now log in using your new password.");

    }

    //    Change Password
    @Transactional
    public GenericMessageResponse changePassword(ChangePasswordRequest request) {
        logger.info("Password change request received");
        User user =  currentUserService.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentials("The password you entered is incorrect.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Your new password must be different from your current password.");
        }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            usersRepository.save(user);
            return new GenericMessageResponse("Password Changed Successfully.");
        }
    }

