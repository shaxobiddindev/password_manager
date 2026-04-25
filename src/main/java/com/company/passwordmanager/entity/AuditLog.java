package com.company.passwordmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String username;
    private Long vaultItemId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }

    public enum Action {
        REGISTER, LOGIN, LOGOUT,
        VIEW, COPY,
        CREATE, UPDATE, DELETE,
        GENERATE_PASSWORD, CHECK_STRENGTH
    }
}
