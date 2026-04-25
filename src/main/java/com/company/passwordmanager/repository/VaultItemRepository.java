package com.company.passwordmanager.repository;

import com.company.passwordmanager.entity.VaultItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VaultItemRepository extends JpaRepository<VaultItem, Long> {
    List<VaultItem> findAllByUserId(Long userId);
    Optional<VaultItem> findByIdAndUserId(Long id, Long userId);
    List<VaultItem> findAllByUserIdAndCategory(Long userId, String category);
    boolean existsByIdAndUserId(Long id, Long userId);
}
