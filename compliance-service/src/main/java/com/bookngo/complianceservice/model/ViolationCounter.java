package com.bookngo.complianceservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** One row per user: how many violations in a row, and how many strikes so far. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "violation_counters")
public class ViolationCounter {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "consecutive_violations", nullable = false)
    private short consecutiveViolations;

    @Column(name = "strike_count", nullable = false)
    private short strikeCount;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public ViolationCounter(Long userId) {
        this.userId = userId;
    }
}
