package com.company.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultItemResponse {
    private Long id;
    private String serviceName;
    private String url;
    private String username;
    private String category;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long reuseCount;
    private java.util.List<String> sharedWithUsernames;
    private boolean shareWithAdmins;
    private String ownerName;
    @com.fasterxml.jackson.annotation.JsonProperty("isOwner")
    private boolean isOwner;
    // Password is intentionally excluded for security
}
