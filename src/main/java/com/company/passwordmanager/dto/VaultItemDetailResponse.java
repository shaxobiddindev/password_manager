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
public class VaultItemDetailResponse {
    private Long id;
    private String serviceName;
    private String url;
    private String username;
    private String password; // decrypted — only returned on explicit detail request
    private String category;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.util.List<String> sharedWithUsernames;
    private boolean shareWithAdmins;
    private String ownerName;
    @com.fasterxml.jackson.annotation.JsonProperty("isOwner")
    private boolean isOwner;
}
