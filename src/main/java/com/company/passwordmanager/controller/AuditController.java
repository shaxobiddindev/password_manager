package com.company.passwordmanager.controller;

import com.company.passwordmanager.dto.AuditLogResponse;
import com.company.passwordmanager.repository.UserRepository;
import com.company.passwordmanager.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logs", description = "View audit logs for actions performed on the vault")
public class AuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    @Operation(summary = "Get my audit logs", description = "Returns all audit logs for the currently authenticated user")
    public ResponseEntity<List<AuditLogResponse>> getMyLogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.ok(auditService.getLogsForUser(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all audit logs (Admin only)", description = "Returns all audit logs across all users. Requires ADMIN role.")
    public ResponseEntity<List<AuditLogResponse>> getAllLogs() {
        return ResponseEntity.ok(auditService.getAllLogs());
    }

    @GetMapping("/vault/{vaultItemId}")
    @Operation(summary = "Get audit logs for a vault item", description = "Returns audit logs for a specific vault item")
    public ResponseEntity<List<AuditLogResponse>> getVaultItemLogs(
            @PathVariable Long vaultItemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(auditService.getLogsForVaultItem(vaultItemId));
    }

    private Long resolveUserId(String username) {
        return userRepository.findByLogin(username)
                .or(() -> userRepository.findByEmail(username))
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
