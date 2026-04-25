package com.company.passwordmanager.controller;

import com.company.passwordmanager.dto.VaultItemDetailResponse;
import com.company.passwordmanager.dto.VaultItemRequest;
import com.company.passwordmanager.dto.VaultItemResponse;
import com.company.passwordmanager.service.VaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/vault")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vault", description = "Manage your encrypted password vault")
public class VaultController {

    private final VaultService vaultService;

    @GetMapping
    @Operation(summary = "List all vault items", description = "Returns all vault items without passwords")
    public ResponseEntity<List<VaultItemResponse>> getAllItems(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(vaultService.getAllItems(userDetails.getUsername()));
    }

    @PostMapping
    @Operation(summary = "Create vault item", description = "Creates a new vault item; password is AES-encrypted before storage")
    public ResponseEntity<VaultItemResponse> createItem(
            @Valid @RequestBody VaultItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.createItem(request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vault item detail", description = "Returns full vault item including decrypted password. Audit log is recorded.")
    public ResponseEntity<VaultItemDetailResponse> getItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(vaultService.getItem(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vault item", description = "Updates vault item. If password is provided it will be re-encrypted.")
    public ResponseEntity<VaultItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody VaultItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(vaultService.updateItem(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vault item", description = "Permanently deletes a vault item")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        vaultService.deleteItem(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "Record copy action", description = "Logs a COPY audit event when user copies a password in the frontend")
    public ResponseEntity<Void> recordCopy(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        vaultService.recordCopy(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
