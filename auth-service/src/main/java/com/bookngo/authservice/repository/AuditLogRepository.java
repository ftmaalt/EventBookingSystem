package com.bookngo.authservice.repository;

import com.bookngo.authservice.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    java.util.Optional<AuditLog> findFirstByActionAndDetails(String action, String details);

}
