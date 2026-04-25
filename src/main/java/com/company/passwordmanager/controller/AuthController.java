package com.company.passwordmanager.controller;

import com.company.passwordmanager.dto.AuthResponse;
import com.company.passwordmanager.dto.ChangePasswordRequest;
import com.company.passwordmanager.dto.LoginRequest;
import com.company.passwordmanager.dto.UnlockRequest;
import com.company.passwordmanager.dto.RegisterRequest;
import com.company.passwordmanager.dto.UserResponse;
import com.company.passwordmanager.dto.UserSearchResponse;
import com.company.passwordmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user by email/login and password, returns a JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/unlock")
    @Operation(summary = "Unlock Vault", description = "Secondary authentication to unlock sensitive data using master password")
    public ResponseEntity<?> unlock(@Valid @RequestBody UnlockRequest request) {
        authService.unlock(request);
        return ResponseEntity.ok().body("{\"status\": \"success\"}");
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Returns the email and role of the authenticated user")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change master password", description = "Updates the user's master password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-user")
    @Operation(summary = "Verify user existence", description = "Checks if a user exists by username or email for sharing purposes")
    public ResponseEntity<UserSearchResponse> checkUser(@RequestParam String query) {
        return ResponseEntity.ok(authService.searchUser(query));
    }

    @GetMapping("/search-users")
    @Operation(summary = "Search users", description = "Returns a list of users matching the query for autocomplete")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(authService.searchUsers(query));
    }
}
