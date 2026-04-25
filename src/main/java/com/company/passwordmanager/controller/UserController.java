package com.company.passwordmanager.controller;

import com.company.passwordmanager.dto.UserResponse;
import com.company.passwordmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management for admins")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns a list of all registered users (ADMIN only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{login}/role")
    @Operation(summary = "Update user role", description = "Updates the role of a user (ADMIN only)")
    public ResponseEntity<Void> updateRole(@PathVariable String login, @RequestParam String role) {
        userService.updateRole(login, role);
        return ResponseEntity.ok().build();
    }
}
