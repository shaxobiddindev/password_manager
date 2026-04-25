package com.company.passwordmanager.controller;

import com.company.passwordmanager.dto.PasswordGenerateRequest;
import com.company.passwordmanager.dto.PasswordStrengthRequest;
import com.company.passwordmanager.dto.PasswordStrengthResponse;
import com.company.passwordmanager.service.AuditService;
import com.company.passwordmanager.service.PasswordService;
import com.company.passwordmanager.entity.AuditLog;
import com.company.passwordmanager.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/password")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Password Tools", description = "Password generation, strength check, and reuse detection")
public class PasswordController {

    private final PasswordService passwordService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    @Operation(summary = "Generate a secure password",
            description = "Generates a random password based on provided config (length, charset options)")
    public ResponseEntity<Map<String, String>> generatePassword(
            @Valid @RequestBody PasswordGenerateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String generated = passwordService.generatePassword(request);

        // Audit log
        userRepository.findByLogin(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .ifPresent(u -> auditService.log(u.getId(),
                        AuditLog.Action.GENERATE_PASSWORD,
                        "Generated password of length " + request.getLength()));

        return ResponseEntity.ok(Map.of("password", generated));
    }

    @PostMapping("/strength")
    @Operation(summary = "Check password strength",
            description = "Analyzes password for strength and detects reuse in your vault")
    public ResponseEntity<PasswordStrengthResponse> checkStrength(
            @Valid @RequestBody PasswordStrengthRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        PasswordStrengthResponse response =
                passwordService.checkStrength(request, userDetails.getUsername());

        // Audit log
        userRepository.findByLogin(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .ifPresent(u -> auditService.log(u.getId(),
                        AuditLog.Action.CHECK_STRENGTH,
                        "Checked password strength: " + response.getStrength()));

        return ResponseEntity.ok(response);
    }
}
