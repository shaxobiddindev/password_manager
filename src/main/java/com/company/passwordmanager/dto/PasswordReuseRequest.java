package com.company.passwordmanager.dto;

import lombok.Data;

@Data
public class PasswordReuseRequest {
    private String password;
    private Long excludeId;
}
