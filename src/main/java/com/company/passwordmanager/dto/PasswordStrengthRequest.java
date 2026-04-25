package com.company.passwordmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordStrengthRequest {

    @NotBlank(message = "Password is required")
    private String password;
}
