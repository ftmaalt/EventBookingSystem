package com.project.bookngo.model;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    @Column(name = "target_entity", nullable = false, length = 100)
    private String targetEntity;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;


    public AuditLog() {
    }

    public AuditLog(String action, String performedBy, String targetEntity, Long targetId, String details, LocalDateTime timestamp) {
        this.action = action;
        this.performedBy = performedBy;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = timestamp;
    }
}
