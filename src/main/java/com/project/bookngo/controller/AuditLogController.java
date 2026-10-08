package com.project.bookngo.controller;

import com.project.bookngo.model.AuditLog;
import com.project.bookngo.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieves the application's audit log.
     *
     * @param pageable pagination and sorting configuration
     * @return paginated audit log records
     */
    @GetMapping
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogService.getAuditLogs(pageable);
    }
}