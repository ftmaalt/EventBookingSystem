package com.project.bookngo.service;

import com.project.bookngo.model.AuditLog;
import com.project.bookngo.repository.AuditLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;


    /**
     * Creates and persists an audit record for an important application action.
     *
     * @param action the action that was performed
     * @param performedBy the user who performed the action
     * @param targetEntity the type of entity affected
     * @param targetId the ID of the affected entity
     * @param details additional information about the action
     * @return the persisted audit log
     */
    @Transactional
    public AuditLog logAction(String action, String performedBy, String targetEntity, Long targetId, String details) {
        AuditLog auditLog = new AuditLog(action, performedBy, targetEntity, targetId, details, LocalDateTime.now());

        return auditLogRepository.save(auditLog);
    }

    /**
     * Retrieves audit logs using pagination and sorting.
     *
     * @param pageable pagination and sorting configuration
     * @return a page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
