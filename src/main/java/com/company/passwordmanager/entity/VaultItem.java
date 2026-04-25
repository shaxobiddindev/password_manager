package com.company.passwordmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vault_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String serviceName;

    private String url;

    private String username;

    @Column(nullable = false)
    private String encryptedPassword;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "vault_item_sharing",
        joinColumns = @JoinColumn(name = "vault_item_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> sharedWith = new HashSet<>();

    @Builder.Default
    private boolean shareWithAdmins = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
