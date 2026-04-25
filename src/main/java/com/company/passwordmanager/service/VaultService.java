package com.company.passwordmanager.service;

import com.company.passwordmanager.dto.PasswordReuseRequest;
import com.company.passwordmanager.dto.PasswordReuseResponse;
import com.company.passwordmanager.dto.VaultItemDetailResponse;
import com.company.passwordmanager.dto.VaultItemRequest;
import com.company.passwordmanager.dto.VaultItemResponse;
import com.company.passwordmanager.dto.VaultStatsResponse;
import com.company.passwordmanager.entity.User;
import com.company.passwordmanager.entity.VaultItem;
import com.company.passwordmanager.exception.ResourceNotFoundException;
import com.company.passwordmanager.exception.UnauthorizedException;
import com.company.passwordmanager.repository.UserRepository;
import com.company.passwordmanager.repository.VaultItemRepository;
import com.company.passwordmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultItemRepository vaultItemRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final AuditService auditService;

    @Transactional
    public VaultItemResponse createItem(VaultItemRequest request, String username) {
        User user = resolveUser(username);

        VaultItem.Visibility visibility = VaultItem.Visibility.ALL;
        if (request.getVisibility() != null) {
            try {
                visibility = VaultItem.Visibility.valueOf(request.getVisibility().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        VaultItem item = VaultItem.builder()
                .owner(user)
                .serviceName(request.getServiceName())
                .url(request.getUrl())
                .username(request.getUsername())
                .encryptedPassword(encryptionUtil.encrypt(request.getPassword()))
                .notes(request.getNotes())
                .category(request.getCategory())
                .visibility(visibility)
                .build();

        item = vaultItemRepository.save(item);
        log.debug("Created vault item id={} for user={}", item.getId(), username);

        auditService.logCreate(user.getId(), item.getId(), item.getServiceName());

        return toResponse(item, user);
    }

    @Transactional(readOnly = true)
    public List<VaultItemResponse> getAllItems(String username) {
        User user = resolveUser(username);
        List<VaultItem> allItems = vaultItemRepository.findAll();

        // Filter based on role and visibility
        List<VaultItem> visibleItems = allItems.stream()
                .filter(item -> {
                    if (user.getRole() == User.Role.ADMIN) return true;
                    return item.getVisibility() == VaultItem.Visibility.ALL;
                })
                .collect(Collectors.toList());

        // Decrypt once per item, map id -> decrypted password
        Map<Long, String> decryptedMap = new HashMap<>();
        for (VaultItem item : visibleItems) {
            decryptedMap.put(item.getId(), encryptionUtil.decrypt(item.getEncryptedPassword()));
        }

        // Count occurrences of each unique password value
        Map<String, Long> passwordFrequency = decryptedMap.values().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        // Build responses with reuseCount
        return visibleItems.stream()
                .map(item -> {
                    String decrypted = decryptedMap.get(item.getId());
                    long reuseCount = passwordFrequency.getOrDefault(decrypted, 1L) - 1;
                    VaultItemResponse res = toResponse(item, user);
                    res.setReuseCount(reuseCount);
                    return res;
                })
                .sorted(Comparator.comparing(VaultItemResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public VaultItemDetailResponse getItem(Long id, String username) {
        User user = resolveUser(username);
        VaultItem item = findAndCheckViewPermission(id, user);

        auditService.logView(user.getId(), item.getId());

        return toDetailResponse(item, user);
    }

    @Transactional
    public VaultItemResponse updateItem(Long id, VaultItemRequest request, String username) {
        User user = resolveUser(username);
        VaultItem item = findAndCheckEditPermission(id, user);

        item.setServiceName(request.getServiceName());
        item.setUrl(request.getUrl());
        item.setUsername(request.getUsername());
        item.setNotes(request.getNotes());
        item.setCategory(request.getCategory());

        if (request.getVisibility() != null) {
            try {
                item.setVisibility(VaultItem.Visibility.valueOf(request.getVisibility().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            item.setEncryptedPassword(encryptionUtil.encrypt(request.getPassword()));
        }

        item = vaultItemRepository.save(item);
        log.debug("Updated vault item id={} for user={}", item.getId(), username);

        auditService.logUpdate(user.getId(), item.getId(), item.getServiceName());

        return toResponse(item, user);
    }

    @Transactional
    public void deleteItem(Long id, String username) {
        User user = resolveUser(username);
        VaultItem item = findAndCheckEditPermission(id, user);

        String serviceName = item.getServiceName();
        vaultItemRepository.delete(item);

        log.debug("Deleted vault item id={} for user={}", id, username);
        auditService.logDelete(user.getId(), id, serviceName);
    }

    @Transactional
    public void recordCopy(Long id, String username) {
        User user = resolveUser(username);
        VaultItem item = findAndCheckViewPermission(id, user);
        auditService.logCopy(user.getId(), item.getId());
    }

    @Transactional(readOnly = true)
    public List<VaultItemDetailResponse> exportVault(String username) {
        User user = resolveUser(username);
        List<VaultItem> allItems = vaultItemRepository.findAll();
        
        return allItems.stream()
                .filter(item -> {
                    if (user.getRole() == User.Role.ADMIN) return true;
                    return item.getVisibility() == VaultItem.Visibility.ALL;
                })
                .map(i -> toDetailResponse(i, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VaultStatsResponse getStats(String username) {
        User user = resolveUser(username);
        List<VaultItem> items = vaultItemRepository.findAll().stream()
                .filter(item -> {
                    if (user.getRole() == User.Role.ADMIN) return true;
                    return item.getVisibility() == VaultItem.Visibility.ALL;
                })
                .collect(Collectors.toList());
        
        long total = items.size();
        List<String> passwords = items.stream()
                .map(i -> encryptionUtil.decrypt(i.getEncryptedPassword()))
                .collect(Collectors.toList());

        long weak = passwords.stream().filter(p -> p.length() < 8).count();
        
        long reused = 0;
        java.util.Map<String, Long> counts = passwords.stream()
                .collect(Collectors.groupingBy(java.util.function.Function.identity(), Collectors.counting()));
        reused = counts.values().stream().filter(c -> c > 1).mapToLong(c -> c).sum();

        return VaultStatsResponse.builder()
                .total(total)
                .weak(weak)
                .reused(reused)
                .build();
    }

    @Transactional(readOnly = true)
    public PasswordReuseResponse checkReuse(String password, Long excludeId, String username) {
        User user = resolveUser(username);
        List<PasswordReuseResponse.ReuseItem> reuseItems = vaultItemRepository.findAll().stream()
                .filter(item -> {
                    if (user.getRole() == User.Role.ADMIN) return true;
                    return item.getVisibility() == VaultItem.Visibility.ALL;
                })
                .filter(i -> excludeId == null || !i.getId().equals(excludeId))
                .filter(i -> encryptionUtil.decrypt(i.getEncryptedPassword()).equals(password))
                .map(i -> PasswordReuseResponse.ReuseItem.builder()
                        .id(i.getId())
                        .serviceName(i.getServiceName())
                        .category(i.getCategory())
                        .build())
                .collect(Collectors.toList());

        return PasswordReuseResponse.builder()
                .reused(!reuseItems.isEmpty())
                .count(reuseItems.size())
                .items(reuseItems)
                .build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User resolveUser(String username) {
        return userRepository.findByLogin(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private VaultItem findAndCheckViewPermission(Long itemId, User user) {
        VaultItem item = vaultItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item not found with id: " + itemId));

        if (user.getRole() != User.Role.ADMIN && item.getVisibility() == VaultItem.Visibility.ADMIN_ONLY) {
            throw new UnauthorizedException("You do not have permission to view this item");
        }
        return item;
    }

    private VaultItem findAndCheckEditPermission(Long itemId, User user) {
        VaultItem item = vaultItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault item not found with id: " + itemId));

        if (user.getRole() == User.Role.ADMIN) return item;
        
        if (!item.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the owner or an admin can edit/delete this item");
        }
        return item;
    }

    private VaultItemResponse toResponse(VaultItem item, User currentUser) {
        return VaultItemResponse.builder()
                .id(item.getId())
                .serviceName(item.getServiceName())
                .url(item.getUrl())
                .username(item.getUsername())
                .category(item.getCategory())
                .notes(item.getNotes())
                .visibility(item.getVisibility().name())
                .ownerName(item.getOwner().getLogin())
                .isOwner(item.getOwner().getId().equals(currentUser.getId()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private VaultItemDetailResponse toDetailResponse(VaultItem item, User currentUser) {
        return VaultItemDetailResponse.builder()
                .id(item.getId())
                .serviceName(item.getServiceName())
                .url(item.getUrl())
                .username(item.getUsername())
                .password(encryptionUtil.decrypt(item.getEncryptedPassword()))
                .category(item.getCategory())
                .notes(item.getNotes())
                .visibility(item.getVisibility().name())
                .ownerName(item.getOwner().getLogin())
                .isOwner(item.getOwner().getId().equals(currentUser.getId()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
