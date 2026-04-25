package com.company.passwordmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnlockRequest {
    @NotBlank(message = "Master password is required")
    private String masterPassword;
}
