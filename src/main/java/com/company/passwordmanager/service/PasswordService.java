package com.company.passwordmanager.service;

import com.company.passwordmanager.dto.PasswordGenerateRequest;
import com.company.passwordmanager.dto.PasswordStrengthRequest;
import com.company.passwordmanager.dto.PasswordStrengthResponse;
import com.company.passwordmanager.entity.VaultItem;
import com.company.passwordmanager.repository.UserRepository;
import com.company.passwordmanager.repository.VaultItemRepository;
import com.company.passwordmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS   = "0123456789";
    private static final String SYMBOLS   = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private final SecureRandom secureRandom = new SecureRandom();
    private final VaultItemRepository vaultItemRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    public String generatePassword(PasswordGenerateRequest request) {
        StringBuilder charPool = new StringBuilder();
        List<Character> guaranteed = new ArrayList<>();

        if (request.isUppercase()) {
            charPool.append(UPPERCASE);
            guaranteed.add(UPPERCASE.charAt(secureRandom.nextInt(UPPERCASE.length())));
        }
        if (request.isLowercase()) {
            charPool.append(LOWERCASE);
            guaranteed.add(LOWERCASE.charAt(secureRandom.nextInt(LOWERCASE.length())));
        }
        if (request.isNumbers()) {
            charPool.append(NUMBERS);
            guaranteed.add(NUMBERS.charAt(secureRandom.nextInt(NUMBERS.length())));
        }
        if (request.isSymbols()) {
            charPool.append(SYMBOLS);
            guaranteed.add(SYMBOLS.charAt(secureRandom.nextInt(SYMBOLS.length())));
        }

        if (charPool.isEmpty()) {
            charPool.append(LOWERCASE);
            guaranteed.add(LOWERCASE.charAt(secureRandom.nextInt(LOWERCASE.length())));
        }

        String pool = charPool.toString();
        List<Character> passwordChars = new ArrayList<>(guaranteed);

        for (int i = passwordChars.size(); i < request.getLength(); i++) {
            passwordChars.add(pool.charAt(secureRandom.nextInt(pool.length())));
        }

        Collections.shuffle(passwordChars, secureRandom);

        StringBuilder password = new StringBuilder();
        for (char c : passwordChars) {
            password.append(c);
        }

        log.debug("Generated password of length {}", request.getLength());
        return password.toString();
    }

    @Transactional(readOnly = true)
    public PasswordStrengthResponse checkStrength(PasswordStrengthRequest request, String username) {
        String password = request.getPassword();

        boolean hasUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower   = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol  = password.chars().anyMatch(c -> SYMBOLS.indexOf(c) >= 0);
        boolean longEnough = password.length() >= 12;

        int score = 0;
        if (hasUpper)   score += 20;
        if (hasLower)   score += 20;
        if (hasDigit)   score += 20;
        if (hasSymbol)  score += 20;
        if (longEnough) score += 20;

        String strength;
        String suggestion;

        if (score <= 20) {
            strength   = "WEAK";
            suggestion = "Add uppercase, lowercase, numbers and symbols. Use at least 12 characters.";
        } else if (score <= 40) {
            strength   = "FAIR";
            suggestion = "Consider adding more character types and increasing length.";
        } else if (score <= 60) {
            strength   = "FAIR";
            suggestion = "Good start! Add symbols or increase length for better security.";
        } else if (score <= 80) {
            strength   = "STRONG";
            suggestion = "Strong password! Consider making it even longer.";
        } else {
            strength   = "VERY_STRONG";
            suggestion = "Excellent password!";
        }

        boolean reused = detectReuse(password, username);
        if (reused) {
            suggestion = "⚠️ This password is already used in your vault. Use a unique password!";
        }

        return PasswordStrengthResponse.builder()
                .strength(strength)
                .score(score)
                .hasUppercase(hasUpper)
                .hasLowercase(hasLower)
                .hasNumbers(hasDigit)
                .hasSymbols(hasSymbol)
                .isLongEnough(longEnough)
                .isReused(reused)
                .suggestion(suggestion)
                .build();
    }

    private boolean detectReuse(String plainPassword, String username) {
        try {
            com.company.passwordmanager.entity.User user = userRepository.findByLogin(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElse(null);
            
            if (user == null) return false;

            List<VaultItem> items = vaultItemRepository.findAll().stream()
                    .filter(item -> {
                        if (user.getRole() == com.company.passwordmanager.entity.User.Role.ADMIN) return true;
                        return item.getVisibility() == com.company.passwordmanager.entity.VaultItem.Visibility.ALL;
                    })
                    .collect(Collectors.toList());

            String encryptedInput = encryptionUtil.encrypt(plainPassword);

            return items.stream()
                    .anyMatch(item -> item.getEncryptedPassword().equals(encryptedInput));
        } catch (Exception e) {
            log.warn("Reuse detection failed: {}", e.getMessage());
            return false;
        }
    }
}
