package com.project.bookngo.controller;

import com.project.bookngo.model.request.*;
import com.project.bookngo.model.response.ForgotPasswordResponse;
import com.project.bookngo.model.response.LoginResponse;
import com.project.bookngo.model.response.RegisterResponse;
import com.project.bookngo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
//register
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("Calling register==>");
        RegisterResponse registerResponse=authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    @GetMapping("/verify")
    public ResponseEntity<RegisterResponse> verifyEmail(@RequestParam String token) {
        System.out.println("Calling verifyEmail==>");
       RegisterResponse registerResponse= authService.verifyEmail(token);
       return ResponseEntity.status(HttpStatus.OK).body(registerResponse);
    }

//    Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("Calling login==>");
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }
//        Password Reset
    @PostMapping("/forgotPassword")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        System.out.println("Calling forgotPassword==>");
        ForgotPasswordResponse forgotPasswordResponse= authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.status(HttpStatus.OK).body(forgotPasswordResponse);
        }
        @PostMapping("/resetPassword")
    public ResponseEntity<ForgotPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest passwordRequest){
            System.out.println("Calling resetPassword==>");
            ForgotPasswordResponse passwordResponse= authService.resetPassword(passwordRequest);
            return ResponseEntity.status(HttpStatus.OK).body(passwordResponse);
        }
//        Change Password
    @PutMapping("/changePassword")
    public ResponseEntity<ForgotPasswordResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        System.out.println("Calling changePassword==>");
        ForgotPasswordResponse passwordResponse= authService.changePassword(request);
        return ResponseEntity.status(HttpStatus.OK).body(passwordResponse);
    }
}