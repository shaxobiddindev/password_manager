package com.company.passwordmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VaultItemRequest {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    private String url;

    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String notes;

    private String category;
    private java.util.List<String> sharedWith; // List of usernames/emails
    private boolean shareWithAdmins;
}
