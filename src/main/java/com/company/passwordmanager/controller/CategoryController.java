package com.company.passwordmanager.controller;

import com.company.passwordmanager.repository.VaultItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categories", description = "Manage vault item categories")
public class CategoryController {

    private final VaultItemRepository vaultItemRepository;
    private final com.company.passwordmanager.repository.UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all user categories", description = "Returns a unique list of categories used by the user")
    public ResponseEntity<List<String>> getCategories(@AuthenticationPrincipal UserDetails userDetails) {
        com.company.passwordmanager.entity.User user = userRepository.findByLogin(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .get();
        
        List<String> categories = vaultItemRepository.findAll().stream()
                .filter(item -> {
                    if (user.getRole() == com.company.passwordmanager.entity.User.Role.ADMIN) return true;
                    return item.getVisibility() == com.company.passwordmanager.entity.VaultItem.Visibility.ALL;
                })
                .map(i -> i.getCategory())
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(categories);
    }
}
