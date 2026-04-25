package com.company.passwordmanager.dto;

import com.company.passwordmanager.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private Long vaultItemId;
    private AuditLog.Action action;
    private String details;
    private LocalDateTime timestamp;
}
