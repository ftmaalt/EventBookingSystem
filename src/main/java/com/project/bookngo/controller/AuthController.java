package com.project.bookngo.controller;

import com.project.bookngo.model.request.LoginRequest;
import com.project.bookngo.model.request.RegisterRequest;
import com.project.bookngo.model.response.LoginResponse;
import com.project.bookngo.model.response.RegisterResponse;
import com.project.bookngo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

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
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("Calling login==>");
        LoginResponse loginResponse= authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }
}