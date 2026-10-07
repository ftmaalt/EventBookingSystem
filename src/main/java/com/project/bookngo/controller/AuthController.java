package com.project.bookngo.controller;

import com.project.bookngo.model.request.*;
import com.project.bookngo.model.response.LoginResponse;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    //register
    @PostMapping("/register")
    public ResponseEntity<GenericMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Calling register==>");
        GenericMessageResponse registerResponse=authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    @GetMapping("/verify")
    public ResponseEntity<GenericMessageResponse> verifyEmail(@RequestParam String token) {
        logger.info("Calling verifyEmail==>");
        GenericMessageResponse registerResponse= authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(registerResponse);
    }

    //    Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        logger.info("Calling login==>");
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }
    //        Password Reset
    @PostMapping("/forgotPassword")
    public ResponseEntity<GenericMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        logger.info("Calling forgotPassword==>");
        GenericMessageResponse forgotPasswordResponse= authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.status(HttpStatus.OK).body(forgotPasswordResponse);
    }
    @PostMapping("/resetPassword")
    public ResponseEntity<GenericMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest passwordRequest){
        logger.info("Calling resetPassword==>");
        GenericMessageResponse passwordResponse= authService.resetPassword(passwordRequest);
        return ResponseEntity.status(HttpStatus.OK).body(passwordResponse);
    }
    //        Change Password
    @PutMapping("/changePassword")
    public ResponseEntity<GenericMessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        logger.info("Calling changePassword==>");
        GenericMessageResponse passwordResponse= authService.changePassword(request);
        return ResponseEntity.status(HttpStatus.OK).body(passwordResponse);
    }
}