package com.bookngo.complianceservice.model;

import com.bookngo.complianceservice.model.enums.ViolationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "violations")
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType type;

    @Column(nullable = false)
    private Boolean excused;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Ids from other services: plain columns, no relations.
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;
}
