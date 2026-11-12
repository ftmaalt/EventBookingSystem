package com.bookngo.bookingservice.model;

import com.bookngo.bookingservice.model.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sessions")
public class Sessions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer spotsLeft;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Owned by catalog-service. Copied when the session is created, so booking never needs a remote call.
    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "activity_title", nullable = false, length = 150)
    private String activityTitle;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "price_per_person", nullable = false, precision = 10, scale = 3)
    private BigDecimal pricePerPerson;
}
