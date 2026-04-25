package com.company.passwordmanager.service;

import com.company.passwordmanager.dto.VaultItemDetailResponse;
import com.company.passwordmanager.dto.VaultItemRequest;
import com.company.passwordmanager.dto.VaultItemResponse;
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

import java.util.List;
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
        return vaultItemRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toResponse)
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
