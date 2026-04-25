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

        VaultItem item = VaultItem.builder()
                .userId(user.getId())
                .serviceName(request.getServiceName())
                .url(request.getUrl())
                .username(request.getUsername())
                .encryptedPassword(encryptionUtil.encrypt(request.getPassword()))
                .notes(request.getNotes())
                .category(request.getCategory())
                .build();

        item = vaultItemRepository.save(item);
        log.debug("Created vault item id={} for user={}", item.getId(), username);

        auditService.logCreate(user.getId(), item.getId(), item.getServiceName());

        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<VaultItemResponse> getAllItems(String username) {
        User user = resolveUser(username);
        List<VaultItem> items = vaultItemRepository.findAllByUserId(user.getId());

        // Decrypt once per item, map id -> decrypted password
        Map<Long, String> decryptedMap = new HashMap<>();
        for (VaultItem item : items) {
            decryptedMap.put(item.getId(), encryptionUtil.decrypt(item.getEncryptedPassword()));
        }

        // Count occurrences of each unique password value
        Map<String, Long> passwordFrequency = decryptedMap.values().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        // Build responses with reuseCount = (how many OTHER items share the same password)
        return items.stream()
                .map(item -> {
                    String decrypted = decryptedMap.get(item.getId());
                    long reuseCount = passwordFrequency.getOrDefault(decrypted, 1L) - 1;
                    VaultItemResponse res = toResponse(item);
                    res.setReuseCount(reuseCount);
                    return res;
                })
                .sorted(Comparator.comparing(VaultItemResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public VaultItemDetailResponse getItem(Long id, String username) {
        User user = resolveUser(username);
        VaultItem item = findItemForUser(id, user.getId());

        auditService.logView(user.getId(), item.getId());

        return toDetailResponse(item);
    }

    @Transactional
    public VaultItemResponse updateItem(Long id, VaultItemRequest request, String username) {
        User user = resolveUser(username);
        VaultItem item = findItemForUser(id, user.getId());

        item.setServiceName(request.getServiceName());
        item.setUrl(request.getUrl());
        item.setUsername(request.getUsername());
        item.setNotes(request.getNotes());
        item.setCategory(request.getCategory());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            item.setEncryptedPassword(encryptionUtil.encrypt(request.getPassword()));
        }

        item = vaultItemRepository.save(item);
        log.debug("Updated vault item id={} for user={}", item.getId(), username);

        auditService.logUpdate(user.getId(), item.getId(), item.getServiceName());

        return toResponse(item);
    }

    @Transactional
    public void deleteItem(Long id, String username) {
        User user = resolveUser(username);
        VaultItem item = findItemForUser(id, user.getId());

        String serviceName = item.getServiceName();
        vaultItemRepository.delete(item);

        log.debug("Deleted vault item id={} for user={}", id, username);
        auditService.logDelete(user.getId(), id, serviceName);
    }

    @Transactional
    public void recordCopy(Long id, String username) {
        User user = resolveUser(username);
        VaultItem item = findItemForUser(id, user.getId());
        auditService.logCopy(user.getId(), item.getId());
    }

    @Transactional(readOnly = true)
    public List<VaultItemDetailResponse> exportVault(String username) {
        User user = resolveUser(username);
        return vaultItemRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VaultStatsResponse getStats(String username) {
        User user = resolveUser(username);
        List<VaultItem> items = vaultItemRepository.findAllByUserId(user.getId());
        
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
        List<PasswordReuseResponse.ReuseItem> reuseItems = vaultItemRepository.findAllByUserId(user.getId())
                .stream()
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

    private VaultItem findItemForUser(Long itemId, Long userId) {
        return vaultItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vault item not found with id: " + itemId));
    }

    private VaultItemResponse toResponse(VaultItem item) {
        return VaultItemResponse.builder()
                .id(item.getId())
                .serviceName(item.getServiceName())
                .url(item.getUrl())
                .username(item.getUsername())
                .category(item.getCategory())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private VaultItemDetailResponse toDetailResponse(VaultItem item) {
        return VaultItemDetailResponse.builder()
                .id(item.getId())
                .serviceName(item.getServiceName())
                .url(item.getUrl())
                .username(item.getUsername())
                .password(encryptionUtil.decrypt(item.getEncryptedPassword()))
                .category(item.getCategory())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
