package com.project.bookngo.controller;

import com.project.bookngo.model.request.UpdateUserRoleRequest;
import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AuthService authService;

    //    Update User Role
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericMessageResponse> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        System.out.println("Calling updateUserRole==>");
        GenericMessageResponse registerResponse= authService.updateUserRole(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(registerResponse);
    }
    // Deactivate User
    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericMessageResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateUser(id));
        // or however your existing AdminController calls into its service —
        // match whatever pattern updateUserRole already uses
    }
}
