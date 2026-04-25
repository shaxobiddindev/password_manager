package com.company.passwordmanager.repository;

import com.company.passwordmanager.entity.VaultItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VaultItemRepository extends JpaRepository<VaultItem, Long> {
    // Shared vault means we filter in service based on role/visibility
}
