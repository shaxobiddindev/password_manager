package com.company.passwordmanager.repository;

import com.company.passwordmanager.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByUserIdOrderByTimestampDesc(Long userId);
    List<AuditLog> findAllByVaultItemIdOrderByTimestampDesc(Long vaultItemId);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
