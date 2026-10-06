package com.project.bookngo.controller;

import com.project.bookngo.model.request.*;
import com.project.bookngo.model.response.LoginResponse;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
//register
    @PostMapping("/register")
    public ResponseEntity<GenericMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("Calling register==>");
        GenericMessageResponse registerResponse=authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    @GetMapping("/verify")
    public ResponseEntity<GenericMessageResponse> verifyEmail(@RequestParam String token) {
        System.out.println("Calling verifyEmail==>");
       GenericMessageResponse registerResponse= authService.verifyEmail(token);
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
    public ResponseEntity<GenericMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        System.out.println("Calling forgotPassword==>");
        GenericMessageResponse forgotPasswordResponse= authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.status(HttpStatus.OK).body(forgotPasswordResponse);
        }
        @PostMapping("/resetPassword")
    public ResponseEntity<GenericMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest passwordRequest){
            System.out.println("Calling resetPassword==>");
            GenericMessageResponse passwordResponse= authService.resetPassword(passwordRequest);
            return ResponseEntity.status(HttpStatus.OK).body(passwordResponse);
        }
//        Change Password
    @PutMapping("/changePassword")
    public ResponseEntity<GenericMessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        System.out.println("Calling changePassword==>");
        GenericMessageResponse passwordResponse= authService.changePassword(request);
        return ResponseEntity.status(HttpStatus.OK).body(passwordResponse);
    }
}