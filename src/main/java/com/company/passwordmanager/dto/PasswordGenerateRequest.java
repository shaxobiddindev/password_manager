package com.company.passwordmanager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PasswordGenerateRequest {

    @Min(value = 6, message = "Length must be at least 6")
    @Max(value = 128, message = "Length must not exceed 128")
    private int length = 16;

    private boolean uppercase = true;
    private boolean lowercase = true;
    private boolean numbers = true;
    private boolean symbols = true;
}
