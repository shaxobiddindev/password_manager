package com.company.passwordmanager.service;

import com.company.passwordmanager.dto.AuditLogResponse;
import com.company.passwordmanager.entity.AuditLog;
import com.company.passwordmanager.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(Long userId, Long vaultItemId, AuditLog.Action action, String details) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .vaultItemId(vaultItemId)
                .action(action)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log saved: userId={}, action={}, vaultItemId={}", userId, action, vaultItemId);
    }

    @Transactional
    public void log(Long userId, AuditLog.Action action, String details) {
        log(userId, null, action, details);
    }

    public void logView(Long userId, Long vaultItemId) {
        log(userId, vaultItemId, AuditLog.Action.VIEW, "User viewed password for vault item: " + vaultItemId);
    }

    public void logCopy(Long userId, Long vaultItemId) {
        log(userId, vaultItemId, AuditLog.Action.COPY, "User copied password for vault item: " + vaultItemId);
    }

    public void logCreate(Long userId, Long vaultItemId, String serviceName) {
        log(userId, vaultItemId, AuditLog.Action.CREATE, "Created vault item: " + serviceName);
    }

    public void logUpdate(Long userId, Long vaultItemId, String serviceName) {
        log(userId, vaultItemId, AuditLog.Action.UPDATE, "Updated vault item: " + serviceName);
    }

    public void logDelete(Long userId, Long vaultItemId, String serviceName) {
        log(userId, vaultItemId, AuditLog.Action.DELETE, "Deleted vault item: " + serviceName);
    }

    public void logRegister(Long userId, String email) {
        log(userId, AuditLog.Action.REGISTER, "User registered with email: " + email);
    }

    public void logLogin(Long userId, String username) {
        log(userId, AuditLog.Action.LOGIN, "User logged in: " + username);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsForUser(Long userId) {
        return auditLogRepository.findAllByUserIdOrderByTimestampDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsForVaultItem(Long vaultItemId) {
        return auditLogRepository.findAllByVaultItemIdOrderByTimestampDesc(vaultItemId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .vaultItemId(log.getVaultItemId())
                .action(log.getAction())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
