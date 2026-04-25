package com.company.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordStrengthResponse {
    private String strength;       // WEAK, FAIR, STRONG, VERY_STRONG
    private int score;             // 0-100
    private boolean hasUppercase;
    private boolean hasLowercase;
    private boolean hasNumbers;
    private boolean hasSymbols;
    private boolean isLongEnough;
    private boolean isReused;
    private String suggestion;
}
