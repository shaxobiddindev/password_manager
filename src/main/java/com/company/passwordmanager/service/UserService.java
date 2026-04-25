package com.company.passwordmanager.service;

import com.company.passwordmanager.dto.UpdateSettingsRequest;
import com.company.passwordmanager.dto.UserResponse;
import com.company.passwordmanager.entity.User;
import com.company.passwordmanager.exception.ResourceNotFoundException;
import com.company.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserResponse.builder()
                        .email(u.getEmail())
                        .login(u.getLogin())
                        .role(u.getRole().name())
                        .autoLockTimer(u.getAutoLockTimer())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateRole(String login, String role) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + login));
        user.setRole(User.Role.valueOf(role.toUpperCase()));
        userRepository.save(user);
    }

    @Transactional
    public void updateSettings(UpdateSettingsRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByLogin(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getAutoLockTimer() != null) {
            user.setAutoLockTimer(request.getAutoLockTimer());
            userRepository.save(user);
            log.info("Settings updated for user: {}", username);
        }
    }
}
