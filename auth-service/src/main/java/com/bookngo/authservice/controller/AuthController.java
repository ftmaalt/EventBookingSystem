
package com.bookngo.authservice.controller;

import com.bookngo.authservice.dto.request.*;
import com.bookngo.authservice.dto.response.GenericMessageResponse;
import com.bookngo.authservice.dto.response.LoginResponse;
import com.bookngo.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    public ResponseEntity<GenericMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received");
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<GenericMessageResponse> verifyEmail(@RequestParam String token) {
        logger.info("Email verification request received");
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received");
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<GenericMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot password request received");
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<GenericMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Password reset request received");
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PutMapping("/changePassword")
    public ResponseEntity<GenericMessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        logger.info("Change password request received");
        return ResponseEntity.ok(authService.changePassword(request));
    }
}
