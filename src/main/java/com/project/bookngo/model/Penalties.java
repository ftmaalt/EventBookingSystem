package com.project.bookngo.model;

import com.project.bookngo.model.enums.PenaltyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "penalties")
public class Penalties {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private Short strikeNumber;

    @Column(precision = 10, scale = 3)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column
    @NotBlank
    private PenaltyStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    @NotBlank
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime paidAt;


//    --- Relationship Mapping ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

}
