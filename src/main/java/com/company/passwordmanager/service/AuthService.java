package com.company.passwordmanager.service;

import com.company.passwordmanager.dto.AuthResponse;
import com.company.passwordmanager.dto.ChangePasswordRequest;
import com.company.passwordmanager.dto.LoginRequest;
import com.company.passwordmanager.dto.RegisterRequest;
import com.company.passwordmanager.dto.UnlockRequest;
import com.company.passwordmanager.dto.UserResponse;
import com.company.passwordmanager.dto.UserSearchResponse;
import com.company.passwordmanager.entity.User;
import com.company.passwordmanager.exception.BadCredentialsException;
import com.company.passwordmanager.exception.DuplicateResourceException;
import com.company.passwordmanager.repository.UserRepository;
import com.company.passwordmanager.security.CustomUserDetailsService;
import com.company.passwordmanager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (request.getLogin() != null && !request.getLogin().isBlank()
                && userRepository.existsByLogin(request.getLogin())) {
            throw new DuplicateResourceException("Login already taken: " + request.getLogin());
        }

        User user = User.builder()
                .email(request.getEmail())
                .login(request.getLogin() != null && !request.getLogin().isBlank()
                        ? request.getLogin() : request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", request.getEmail());

        auditService.logRegister(user.getId(), user.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getLogin());
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        log.info("User logged in: {}", request.getUsername());
        auditService.logLogin(user.getId(), request.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(
                user.getLogin() != null ? user.getLogin() : user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .email(user.getEmail())
                .login(user.getLogin())
                .role(user.getRole().name())
                .autoLockTimer(user.getAutoLockTimer())
                .build();
    }

    @Transactional(readOnly = true)
    public void unlock(UnlockRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByLogin(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getMasterPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Wrong master password");
        }
    }

    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByLogin(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return UserResponse.builder()
                .email(user.getEmail())
                .login(user.getLogin())
                .role(user.getRole().name())
                .autoLockTimer(user.getAutoLockTimer())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByLogin(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", username);
    }

    public UserSearchResponse searchUser(String query) {
        return userRepository.findByLogin(query)
                .or(() -> userRepository.findByEmail(query))
                .filter(u -> u.getRole() != User.Role.ADMIN)
                .map(u -> UserSearchResponse.builder()
                        .found(true)
                        .username(u.getLogin())
                        .email(u.getEmail())
                        .build())
                .orElse(UserSearchResponse.builder()
                        .found(false)
                        .build());
    }

    public List<UserSearchResponse> searchUsers(String query) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        
        return userRepository.findByLoginContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query).stream()
                .filter(u -> u.getRole() != User.Role.ADMIN)
                .limit(10)
                .map(u -> UserSearchResponse.builder()
                        .found(true)
                        .username(u.getLogin())
                        .email(u.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}
