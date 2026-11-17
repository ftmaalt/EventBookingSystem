package com.bookngo.authservice.controller;

import com.bookngo.authservice.dto.request.UpdateUserStatusRequest;
import com.bookngo.authservice.dto.response.InternalUserResponse;
import com.bookngo.authservice.security.InternalKeyValidator;
import com.bookngo.authservice.service.InternalUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/* for other microservices only */
@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;
    private final InternalKeyValidator keyValidator;

    public InternalUserController(InternalUserService internalUserService, InternalKeyValidator keyValidator) {
        this.internalUserService = internalUserService;
        this.keyValidator = keyValidator;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternalUserResponse> getUser(
            @RequestHeader(value = "X-Internal-Key", required = false) String key, @PathVariable Long id) {
        keyValidator.check(key);
        return ResponseEntity.ok(internalUserService.getUser(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<InternalUserResponse> updateStatus(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        keyValidator.check(key);
        return ResponseEntity.ok(internalUserService.updateStatus(id, request.getStatus()));
    }
}
