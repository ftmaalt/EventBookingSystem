package com.bookngo.authservice.controller;


import com.bookngo.authservice.dto.request.UpdateUserRoleRequest;
import com.bookngo.authservice.dto.response.GenericMessageResponse;
import com.bookngo.authservice.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    //    Update User Role
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericMessageResponse> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        logger.info("Calling updateUserRole==>");
        GenericMessageResponse registerResponse= adminService.updateUserRole(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(registerResponse);
    }
    // Deactivate User
    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericMessageResponse> deactivateUser(@PathVariable Long id) {
        logger.info("Calling deactivateUser==>");
        return ResponseEntity.ok(adminService.deactivateUser(id));
    }
}