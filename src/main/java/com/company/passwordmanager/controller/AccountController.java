package com.company.passwordmanager.controller;

import com.company.passwordmanager.dto.UpdateSettingsRequest;
import com.company.passwordmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account", description = "User account and profile settings")
public class AccountController {

    private final UserService userService;

    @PutMapping("/settings")
    @Operation(summary = "Update user settings", description = "Updates settings like auto-lock timer")
    public ResponseEntity<Void> updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        userService.updateSettings(request);
        return ResponseEntity.ok().build();
    }
}
