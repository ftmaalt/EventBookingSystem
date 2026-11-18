package com.bookngo.authservice.dto.response;

import com.bookngo.authservice.model.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String action,
        String performedBy,
        String targetEntity,
        Long targetId,
        String details,
        LocalDateTime timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAction(), log.getPerformedBy(), log.getTargetEntity(),
                log.getTargetId(), log.getDetails(), log.getTimestamp());
    }
}
